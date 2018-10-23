package javax0.geci.fluent.internal;

import javax0.geci.api.GeciException;
import javax0.geci.fluent.tree.Node;
import javax0.geci.fluent.tree.Terminal;
import javax0.geci.fluent.tree.Tree;
import javax0.geci.tools.JavaSource;
import javax0.geci.tools.MethodTool;
import javax0.geci.tools.Tools;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class ClassBuilder {
    private final InterfaceNameFactory ifNameFactory;
    private final MethodCollection methods;
    private final FluentBuilderImpl fluent;
    private String interfaceName;

    /**
     * Create a ClassBuilder that builds the interface and class structure from the fluent definition.
     *
     * @param fluent the fluent definition.
     */
    public ClassBuilder(FluentBuilderImpl fluent) {
        this.ifNameFactory = new InterfaceNameFactory();
        this.fluent = fluent;
        this.methods = fluent.getMethods();
    }

    /**
     * Create a new ClassBuilder that is essentially the clone of the other one.
     *
     * @param that the other class builder
     */
    private ClassBuilder(ClassBuilder that) {
        this.ifNameFactory = that.ifNameFactory;
        this.methods = that.methods;
        this.fluent = that.fluent;
        this.interfaceName = that.interfaceName;
    }


    /**
     * Build the interface and class structure that implements the fluent interface.
     *
     * @return the string of the code that was built up.
     */
    public String build() {
        var list = fluent.getNodes();
        if (list.size() == 0) {
            throw new GeciException("There are no actual calls in the fluent structure.");
        }
        final var tree = new Tree(Node.ONCE, list);
        final var calculator = new LastNodeTypeCalculator(methods);
        final var exitType = calculator.getLastNodeReturnType(list.get(list.size() - 1));
        final var lastInterface = Tools.normalizeTypeName(exitType);
        final var interfaces = build(tree, lastInterface);
        final var code = new JavaSource();
        writeStartMethod(code);
        writeWrapperInterface(code);
        writeWrapperClass(code);
        code.write(interfaces);
        return code.toString();
    }


    /**
     * Write the source code of the start method into the source builder.
     * <p>
     * The start method is a public static method with no argument that creates a new instance of the wrapper
     * class and returns it as the interface type that can be used to start the fluent API structure.
     *
     * @param code to write the start method into
     */
    private void writeStartMethod(JavaSource code) {
        final var startMethod = fluent.getStartMethod() == null ? "start" : fluent.getStartMethod();
        final String lastType;
        if (fluent.getLastType() != null) {
            lastType = fluent.getLastType();
            code.write("public interface %s extends %s {}", lastType, ifNameFactory.getLastName());
        } else {
            lastType = ifNameFactory.getLastName();
        }
        try (final var mtBl = code.method(startMethod).modifiers("public static").returnType(lastType).noArgs()) {
            mtBl.statement("return new Wrapper()");
        }
    }

    private void writeWrapperInterface(JavaSource code) {
        if (methods.needWrapperInterface()) {
            try (var ifBl = code.open("public interface WrapperInterface")) {
            }
        }
    }

    /**
     * Write the source code of the wrapper class into the source builder.
     *
     * @param code to write the start method into
     */
    private void writeWrapperClass(JavaSource code) {
        try (var klBl = code.open("public static class Wrapper implements %s", setJoin(ifNameFactory.getAllNames(), fluent.getLastType(), fluent.getInterfaces()))) {
            klBl.statement("private final %s that", fluent.getKlass().getCanonicalName());
            if (fluent.getCloner() != null) {
                try (var coBl = klBl.open("public Wrapper(%s that)", fluent.getKlass().getCanonicalName())) {
                    coBl.statement("this.that = that");
                }
            }
            try (var coBl = klBl.open("public Wrapper()")) {
                coBl.statement("this.that = new %s()", fluent.getKlass().getCanonicalName());
            }
            writeWrapperMethods(code);
        }
    }


    private void writeWrapperMethods(JavaSource code) {
        for (var signature : methods.methodSignatures()) {
            var method = methods.get(signature);
            if (fluent.getCloner() == null || !fluent.getCloner().equals(method)) {
                var notFluent = methods.isExitNode(signature) || !methods.isFluentNode(signature);
                var actualReturnType = notFluent ? null : "Wrapper";
                var signatureString = FluentMethodTool
                        .from(fluent.getKlass())
                        .forThe(method)
                        .withType(actualReturnType)
                        .signature();
                try (var mtBl = code.open(signatureString)) {
                    if (notFluent) {
                        writeNonFluentMethodWrapper(method, mtBl);
                    } else {
                        writeWrapperMethodBody(method, mtBl);
                    }
                }
            }
        }
    }

    private void writeWrapperMethodBody(Method method, JavaSource mtBl) {
        var callString = FluentMethodTool.from(fluent.getKlass()).forThe(method).call();
        if (fluent.getCloner() != null) {
            mtBl.statement("var next = new Wrapper(that.%s)", MethodTool.with(fluent.getCloner()).call())
                    .statement("next.that.%s", callString)
                    .returnStatement("next");

        } else {
            mtBl.statement("that.%s", callString)
                    .returnStatement("this");
        }
    }

    private void writeNonFluentMethodWrapper(Method method, JavaSource mtBl) {
        final String returnKw;
        if (method.getReturnType() == Void.TYPE) {
            returnKw = "";
        } else {
            returnKw = "return ";
        }
        var callString = FluentMethodTool.from(fluent.getKlass()).forThe(method).call();
        mtBl.statement("%sthat.%s", returnKw, callString);
    }

    private String build(Node node, String nextInterface) {
        if (node instanceof Terminal) {
            return build((Terminal) node, nextInterface);
        } else {
            return build((Tree) node, nextInterface);
        }
    }

    private String build(Terminal terminal, String nextInterface) {
        interfaceName = ifNameFactory.getNewName();
        var code = new JavaSource();
        var list = InterfaceList.builderFor(methods)
                .when((terminal.getModifier() & (Node.OPTIONAL | Node.ZERO_OR_MORE)) != 0).then(nextInterface, fluent.getInterfaces())
                .buildList();
        try (var ifcB = code.open("interface %s%s ", interfaceName, list)) {
            ifcB.statement(FluentMethodTool
                    .from(fluent.getKlass())
                    .forThe(methods.get(terminal.getMethod()))
                    .withType((terminal.getModifier() & Node.ZERO_OR_MORE) != 0 ? interfaceName : nextInterface)
                    .asInterface()
                    .signature());
        }
        return code.toString();
    }

    private String build(Tree tree, String nextInterface) {
        int modifier = tree.getModifier();
        if (modifier == Node.ONCE) {
            return buildOnce(tree, nextInterface);
        }
        if (modifier == Node.OPTIONAL) {
            return buildOptional(tree, nextInterface);
        }
        if (modifier == Node.ZERO_OR_MORE) {
            return buildZeroOrMore(tree, nextInterface);
        }
        if (modifier == Node.ONE_OF) {
            return buildOneOf(tree, nextInterface);
        }
        if (modifier == Node.ONE_TERMINAL_OF) {
            return buildOneTerminalOf(tree, nextInterface);
        }
        throw new GeciException("Internal error tree " + tree.toString() + " modifier is " + modifier);
    }

    private String buildOneTerminalOf(Tree tree, String nextInterface) {
        this.interfaceName = ifNameFactory.getNewName();
        var code = new JavaSource();
        try (
                var ifcB = code.open("interface %s", this.interfaceName)) {
            List<Node> list = tree.getList();
            for (var node : list) {
                if (node instanceof Tree) {
                    throw new GeciException("Internal error, ON_TERMINAL_OF contains a non-terminal sub.");
                } else {
                    var terminal = (Terminal) node;
                    ifcB.statement(FluentMethodTool
                            .from(fluent.getKlass())
                            .forThe(methods.get(terminal.getMethod()))
                            .withType(nextInterface)
                            .asInterface()
                            .signature());
                }
            }
        }
        return code.toString();
    }

    private String buildOneOf(Tree tree, String nextInterface) {
        List<Node> list = tree.getList();
        var code = new JavaSource();
        var alternativeInterfaces = new HashSet<String>();
        for (var node : list) {
            var builder = new ClassBuilder(this);
            code.write(builder.build(node, nextInterface));
            alternativeInterfaces.add(builder.interfaceName);
        }
        this.interfaceName = ifNameFactory.getNewName();
        var ifs = InterfaceList.builderFor(methods).set(fluent.getInterfaces()).set(alternativeInterfaces).buildList();
        try (var ifcB = code.open("interface %s%s", this.interfaceName, ifs)) {
        }
        return code.toString();
    }

    private String setJoin(Set<String> set, String... other) {
        if (other != null && other.length > 0) {
            var local = new HashSet<>(set);
            local.addAll(Arrays.stream(other).filter(Objects::nonNull).collect(Collectors.toSet()));
            return String.join(",", local);
        } else {
            return String.join(",", set);
        }
    }

    private String buildZeroOrMore(Tree tree, String nextInterface) {
        List<Node> list = tree.getList();
        var code = new JavaSource();
        ClassBuilder lastBuilder = null;
        this.interfaceName = ifNameFactory.getNewName();
        for (var i = list.size() - 1; i >= 0; i--) {
            final var node = list.get(i);
            var builder = new ClassBuilder(this);
            var actualNextInterface = this.interfaceName;
            if (lastBuilder != null) {
                actualNextInterface = lastBuilder.interfaceName;
            }
            code.write(builder.build(node, actualNextInterface));
            lastBuilder = builder;
        }
        if( lastBuilder == null ){
            throw new GeciException("Internal error");
        }
        var ifs = InterfaceList.builderFor(methods).set(nextInterface, lastBuilder.interfaceName, fluent.getInterfaces()).buildList();
        code.write("interface %s%s {}", this.interfaceName, ifs);
        return code.toString();
    }

    private String buildOptional(Tree tree, String nextInterface) {
        List<Node> list = tree.getList();
        var code = new JavaSource();
        ClassBuilder lastBuilder = null;
        this.interfaceName = ifNameFactory.getNewName();
        for (var i = list.size() - 1; i >= 0; i--) {
            final var node = list.get(i);
            var builder = new ClassBuilder(this);
            var actualNextInterface = nextInterface;
            if (lastBuilder != null) {
                actualNextInterface = lastBuilder.interfaceName;
            }
            code.write(builder.build(node, actualNextInterface));
            lastBuilder = builder;
        }
        if( lastBuilder == null ){
            throw new GeciException("Internal error");
        }
        var ifs = InterfaceList.builderFor(methods).set(nextInterface, lastBuilder.interfaceName, fluent.getInterfaces()).buildList();
        code.statement("interface %s%s {}", this.interfaceName, ifs);
        return code.toString();
    }

    private String buildOnce(Tree tree, String nextInterface) {
        List<Node> list = tree.getList();
        var code = new JavaSource();
        ClassBuilder lastBuilder = null;
        for (var i = list.size() - 1; i >= 0; i--) {
            final var node = list.get(i);
            var builder = new ClassBuilder(this);
            var actualNextInterface = nextInterface;
            if (lastBuilder != null) {
                actualNextInterface = lastBuilder.interfaceName;
            }
            code.write(builder.build(node, actualNextInterface));
            lastBuilder = builder;
        }
        if( lastBuilder == null ){
            throw new GeciException("Internal error");
        }
        this.interfaceName = lastBuilder.interfaceName;
        return code.toString();
    }
}

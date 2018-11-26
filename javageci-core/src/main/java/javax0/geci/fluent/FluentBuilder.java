package javax0.geci.fluent;

import javax0.geci.fluent.internal.FluentBuilderImpl;

/**
 * This interface defines the fluent methods that are to be used to define a fluent interface.
 * <p>
 * Some of the methods accept method names as parameters. In case the name identifies the method in the fluentized
 * class then it is enough to specify the name of the method. When a method is overloaded then the method name is
 * not enough. In that case the method has to specified as {@code name} then opening parentheses {@code (} then
 * the types of the arguments separated by commas (no argument variable name, only the types) and then closing
 * paremetheses {@code )}.
 * <p>
 * The type names have to be specified using the full package name and the class name, except those classes that are
 * in the {@code java.*} packages. These classes should be used with the pure name, like {@code String} or
 * {@code Pattern}.
 */
public interface FluentBuilder {
    static FluentBuilder from(Class<?> klass) {
        return new FluentBuilderImpl(klass);

    }

    /**
     * Define the name of the start method. The start method is a {@code public static} method that can be used to
     * instantiate the builder. When you fluentize a class {@code MyClass} and call {@code start("builder")} then
     * you will start a fluent API use with {@code MyClass.builder()}. The start method does not have any parameter
     * in the current implementation.
     *
     * @param method the name of the start method.
     * @return this
     */
    FluentBuilder start(String method);

    /**
     * Define interfaces that all other interfaces in the fluent interface should implement. This can be typically
     * {@link AutoCloseable} when some API uses the structure of the try-with-resources  command to follow the built
     * structures in the generating Java code.
     *
     * @param interfaces the names of the interfaces to be implemented comma separated. This string will be inserted
     *                   into the list of the interfaces that stands after the {@code extends} or {@code implements}
     *                   keyword.
     * @return this
     */
    FluentBuilder implement(String interfaces);

    /**
     * This is a complimentary method that is equivalent to call {@code implement("AutoCloseable")}.
     *
     * @return this
     */
    default FluentBuilder autoCloseable() {
        return exclude("close").implement("AutoCloseable");
    }

    /**
     * Define the top level interface name that will start the fluent API. Other names are
     * generated automatically unless defined by the method {@link #name(String)}
     *
     * @param type the name of the interface
     * @return this
     */
    FluentBuilder fluentType(String type);

    /**
     * Exclude a method from the fluent interface. If a method is excluded it can not be used in the definition of the
     * fluent api and it will not be part of the interfaces and the wrapper class.
     * <p>
     * The caller may exclude more than one method from the fluent API with subsequent calls to {@link #exclude(String)}
     *
     * @param method the name of the method.
     * @return this
     */
    FluentBuilder exclude(String method);

    /**
     * Define the method that clones the current instance of the class that is fluentized. Such a method usually
     * creates a new instance and copies all the fields to the new instance so that fluent building can go on
     * from that instance and all previous instances can be used in case they are needed to build something different.
     *
     * @param method the name of the cloner method. The method should return a new instance of the class and should have
     *               no parameters.
     * @return this
     */
    FluentBuilder cloner(String method);

    /**
     * The method may be called zero or one time in the fluent API at the defined point.
     *
     * @param method the name of the method. For more information see the note in the documentation
     *               of the class {@link FluentBuilder}
     * @return this
     */
    FluentBuilder optional(String method);

    /**
     * The sub expression may be called zero or one times in the fluent API at the defined point.
     *
     * @param sub the fluent api structure used in the expression
     * @return this
     */
    FluentBuilder optional(FluentBuilder sub);

    /**
     * The method may be called one or more time in the fluent API at the defined point.
     *
     * @param method the name of the method. For more information see the note in the documentation
     *               of the class {@link FluentBuilder}
     * @return this
     */
    FluentBuilder oneOrMore(String method);

    /**
     * The sub expression may be called one or more times in the fluent API at the defined point.
     *
     * @param sub the fluent api structure used in the expression
     * @return this
     */
    FluentBuilder oneOrMore(FluentBuilder sub);

    /**
     * The method may be called zero or more time in the fluent API at the defined point.
     *
     * @param method the name of the method. For more information see the note in the documentation
     *               of the class {@link FluentBuilder}
     * @return this
     */
    FluentBuilder zeroOrMore(String method);

    /**
     * The sub expression may be called zero or more times in the fluent API at the defined point.
     *
     * @param sub the fluent api structure used in the expression
     * @return this
     */
    FluentBuilder zeroOrMore(FluentBuilder sub);

    /**
     * The fluent API using code may call one of the methods at this point.
     *
     * @param methods the names of the methods. For more information see the note in the documentation
     *                of the class {@link FluentBuilder}
     * @return this
     */
    FluentBuilder oneOf(String... methods);

    /**
     * The fluent API using code may call one of the sub structures at this point.
     *
     * @param subs the sub structures from which one may be selected by the caller
     * @return this
     */
    FluentBuilder oneOf(FluentBuilder... subs);

    /**
     * The method can be called exactly once at the point.
     *
     * @param method the name of the method. For more information see the note in the documentation
     *               of the class {@link FluentBuilder}
     * @return this
     */
    FluentBuilder one(String method);

    /**
     * The sub structure can be called exactly once at the point.
     *
     * @param sub substructure
     * @return this
     */
    FluentBuilder one(FluentBuilder sub);

    /**
     * The structure at the very point has to use the name as the interface name
     *
     * @param interfaceName the name of the interface to use at this point of the structure. Where the name is not
     *                      defined the fluent api builder generates interface names automatically.
     * @return this
     */
    FluentBuilder name(String interfaceName);
}
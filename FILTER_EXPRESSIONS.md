# Filter expressions

Many of the generators use filter expressions that select certain members from a class. For example you want to map an
object to a `Map` and you use the `mapper` generator. You want to control which fields should be stored into the map.
This can be done through the configuration key `filter` specifying a filter expression. The expression will select
certain fields to be included and exclude others.

## What is a filter expression

A filter expression is a logical expression. For example the filter expression

`public | private`

will select all members (fileds or methods, whichever the code generator works with) that are either `private` or
`public`, but will not select `protected` or package private members. You can use `|` to express OR relation and `&`
to express AND relation. For example the filter expression

`public | private & final`

will select the members that are `public` or `private` but the `private` fields also have to be `final` or else they
will not be selected.

The expressions can use the `!` character to negate the following part and `(` and `)` can also be used to override the
evaluation order. The words and matchers are numerous, and are documented in the following section: 

## Filter words and matchers

Words are simple selectors, like `private` or `package` that will select a member if the access protection of the member
is private or package private. Matchers are regular expression matchers that identify certain feature of a member and
compare it against a regular expression. For example `simpleName ~ /Map$/` will map any class member where the name
ends with the characters `M`, `a` and `p`. Regular expressions are checked using the matcher class `find()` method
therefore the regular expression may match only a substring of the whole string.

### Type words and matchers

The following words and matchers work only with members that are types (classes, interfaces, enums etc.) and must not
apply to fields and methods. If any of these are used in case of something that is not a class then the code will throw
IllegalArgumentException.

* `interface` checks that the certain class type is an interface
* `primitive` checks that the type is primitive
* `annotation` checks that the type is an annotation
* `anonymous` checks that the type is annotation
* `array` checks that the type is an array
* `enum` checks that the type is an enumeration
* `member` checks that the type is a member class, a.k.a. inner and nested classes 
* `local` checks that the type is a local class. Local classes are defined inside a method.
* `extends ~ /regex/` the canonical name of the superclass matches the regular expression. 
* `simpleName ~ /regex/` the simple name of the class matches the regular expression.
* `canonicalName ~ /regex/` the canonical name of the class matches the regular expression.
* Logically there has to be a `name` matcher, and there is, but that can be applied to fields and methods and therefore
  it is listed below with the universal matchers.

### Method only words and matchers

The following words and matchers work only with members that are methods and must not
apply to fields and methods. If any of these are used in case of something that is not a method then the code will throw
IllegalArgumentException.

* `synthetic` checks that the method is synthetic generated by the compiler and not one existing in the source code.
* `synchronized` checks that the method is synchronized
* `native` checks that method is native
* `strict` checks that method is strict
* `default` checks that method is a default method implemented in an interface
* `vararg`  checks that method has variable number of arguments.
* `implements`  checks that method implements a method defined at least in one interface
* `overrides`  checks that method overrides a method of the superclass or any class above that in the inheritance
   chain.
* `returns ~ /regex/`  checks that the canonical name of the return type of the method matches the regular expression 
* `throws ~ /regex/` checks that any of the declared exceptions matches the regular expression. 
* `signature ~ /regex/` checks that the signature of the method matches the regular expression. The signature of the
  method uses the formal argument names `arg0` ,`arg1`,...,`argN`.
  
### Field only words


The following words work only with members that are fields and must not
apply to types and methods. If any of these are used in case of something that is not a field then the code will throw
IllegalArgumentException.

* `transient` checks that the field is transient
* `volatile` checks that the field is volatile

### Universal words and matcher

The following words and matchers can be applies to select any type of members.

* `true` is simply true. It can be used when the filter is used in the configuration of the member overriding the global
  configuration. This will create a filter expression that includes everything. When this is applied in the configuration
  of a specific member the everything is that member.
* `false` is just the opposite of `true`.
* `private` the access protection of the member is private.
* `protected` the access protection of the member is protected
* `package` the access protection of the member is package protected
* `static` the member is static
* `public`  the access protection of the member is public
* `final` the member is final
* `name ~ /regex/` the name of the member matches the regular expression.
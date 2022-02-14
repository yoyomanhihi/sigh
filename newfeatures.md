# Plan of the new features

## 1. New data structure: enumeration

An enumeration is a particular data type in which a variable can only take a restricted number of values. 

```java
enum FractionErrorType {
    DIVIDED_BY_ZERO, 
    OTHER
}
```

In this example, the enumeration is used to represent the type of error of a fraction. The error can either be a DIVIDED_BY_ZERO error or an error of type OTHER. However, it can't take a value outside of these two, and it can't take both values at the same time.

## 2. Data can be stored in options

Sometimes calling an exception can be non desirable. This pattern (found in Haskell or Rust) can be used to handle errors without an exception system.

```java
enum DivisionResult {
    VALUE(Float),               // Successful division
    ERROR(FractionErrorType)    // Error defined above
}
```
Using the enum type defined in the first section, the DivisionResult can only take the value of a float or a FractionErrorType. The caller receives a result, and has to handle all cases (or transfer the error to its own parent).

The size of the data structure will be the size of the option id + the size of the maximum data.

## 3. Functions, classes and structs can take generic types

A generic type is a generic function, class or structure that is parametrized over types. In the following example, the Optional enum takes the type T and gives it as a parameter for its VALUE field.

```java
enum Optional<T> {
    VALUE(T),
    NOTHING
}
```

To deal with this new feature, the compiled code will be implemented once for each usage.


## 4. Basic classes added to the language

A class is a simple struct which can hold function. They are renamed classes for consistency because in most languages, struct is only data. However, there are no inheritance or polymorphism yet, there is juste some namespacing of functions.

```java
public class Fraction<T> { // public visibility of the class
    // Fields
    var num: T
    var denom: T

    // Function
    public fun toDecimal(): DivisionResult {
        // "this" used to access the fields
        if (this.denom == 0) {
            return DivisionResult.ERROR(FractionErrorType.DIVIDED_BY_ZERO)
        }
        else {
            return DivisionResult.VALUE(this.num / this.denom)
        }
    }
}
```

"this" can be used to access the current instance, the implicit first parameter of a function.

Basic visibility is added (public/private). private functions or fields can only be accessed from inside the class. Default is public.

## 5. Functions and operators can be overloaded if they have different signatures.

Primitive types have predefined operators that are compiled inline (a + between two Ints is just a +). Together with function overloading, it allows to define different types of operations. In the following example, the multiplication is defined for two fractions, as well as for a fraction with an int.

```java
class Fraction<T> {
    // Fields
    var num: T
    var denom: T

    public fun toDecimal(): DivisionResult {
        if (this.denom == 0) {
            return DivisionResult.ERROR(FractionErrorType.DIVIDED_BY_ZERO)
        }
        else {
            return DivisionResult.VALUE(this.num / this.denom)
        }
    }

    // Operators
    @commutative // Optional commutative flag
    public fun *(other: Fraction): Fraction {
        return $Fraction(this.num * other.num, this.denom * other.denom)
    }
    @commutative
    public fun *(other: T): Fraction {
        return $Fraction(this.num * other, this.denom)
    }

    // Note: constructor is the operator $<class_name>
    // by default, each parameter is a field of the class, in order, and is stored into it.
    // So the constructor here is useless.
    // It can be useful to hide private fields though if there are some
    // as all functions, it can be overloaded for different parameter combinations
    public fun $Fraction(num: T, denom: T) {
        this.num = num;
        this.denom = denom;
    }
}
```

Optional @commutative flag on BINARY operators: if set, the operation can be reversed. Otherwise, "this" is always at the LEFT of the operator. here, the operation Fraction.*(Int) is defined and is commutative. As a consequence, if we have Int * Fraction, it will also call the function.

When looking up a symbol, the compiler will look for a definition with matching parameter types.

// Example usage
```java
var f1: Fraction<Int> = $Fraction<Int>(3, 4)
var f2: Fraction<Int> = $Fraction<Int>(1, 4)
var f3: Fraction<Int> = f1 * f2 // f3 will be 3/16
```

Note: the class above takes a T parameter for the value, but in the functions the values are operated on. In particular, they are:
* divided: the result is a Float
* mutliplied: the result is a T
* compared with an Int

As a reminder, when compiled, classes/functions/enums with type parameters are implemented once for each type. If one of the destination type does not support the operators/methods used on it, the compilation will fail. Ex: using Fraction<Bool> here will fail, because it will create a "copy" of the class where T is Bool, and it will fail to multiply them. Thus, the place where the Fraction<Bool> is created will be an error, because Bool does not satisfy T's constraints.

Internally, each parameter type thus keeps a list of "required methods". Each time a method (operator or regular function) is called on that type, it is added to the list. When instanciating the type param with a type, it is check to see if all the required methods are implemented, otherwise it returns an error.

```java
var err: Fraction<Bool> = $Fraction<Bool>(false, true) // -> error
```

## 6. Type inference

Explicitely having to write the type everywhere is tedious. When possible, the compiler will try to automatically detect the type. There are various levels of difficulty:

### Basic cases - Easy
Declarations where the type of the RHS is known: simply have to use it for the LHS.

```java
var auto = 5 // Int
var test = false // Bool
var myStr = "hello" // String
var f = $Fraction<Int>(1, 2) // Fraction<Int>
```

### More indirect cases - Medium
Generic types where the type of the RHS is not fully known, but can be guessed from the parameters. This is harder, but still, all the needed information is in the RHS. When evaluating the tree, the parameters will anyway be evaluated first, then the function call, where the inference will be trivial.

```java
var f4 = $Fraction(1, 2) // Fraction<Int>, because since the parameters are T, and we know their type, we can guess T
```

### Indirect cases - Hard
Generic types, but the RHS of the declaration is not enough to deduce the type. The information may arrive later.

```java
var result = Optional.NOTHING // Optional<???>
// [...]
result = Optional.VALUE(42) // ok, now, we know it's Optional<Int>
result = Optional.VALUE(true) // nice try but no, error
```

Harder because we need a way to store "unknown type param" at the declaration, then update it once the information is known. When leaving scope, check if some types are left incomplete. This is feasible but harder. This won't be in the scope of this project, or only if we have time at the end.

## 7. Pattern matching

To make the usage of enums less tedious, some pattern matching capabilities are added to the declarations. Declaration allow pattern matching. It returns a boolean telling whether or note it matched. It can only be used in a condition (if or while). If not, it can only be an identifier LHS (normal affectation).

Example:

```java
if (var VALUE(v) = f.toDecimal()) {
    // use v
}
while (var VALUE(v) = f.toDecimal()) {
    // while the function returns a VALUE, loop
    // here, we have an infinite loop
    // But for example, with iterators, we can return a VALUE and when at the end return a NOTHING, which will stop the loop
}

// -> switch
// To easily test multiple branches at once
switch (f.toDecimal()) {
    case VALUE(v) -> {
        // use v
    }
    case ERROR(e) -> {
        // handle error
    }
    default -> {
        // If nothing else matched.
        // Note: here, it's nothing
        // Also, we can't access the value of the enum here
    }
}
```

inference -> toDecimal returns a DivisionResult. the VALUE field contains a Float, so v is a Float. However, the ERROR field is possible, so this must be used in an if or a while.

## 8. Keywords

Add the following keywords to the language:

* "typeof" returns the type of the parameter
* "sizeof" returns the size in bytes of a value

```java
Type t = typeof(f);
t == Fraction<Int> // -> true

sizeof(42) // -> 4
sizeof(Optional<Int>) // -> 5
```

## 9. For loop

For is an alternative of while that is present in most of nowadays languages. The following syntax will be used:

```java
// i=0, i=1, i=2...
// exit when i >= 10
for (var i = 0 to 10) {
    // Do something

// i=0, i=2, i=4...
// exit when i >= 10
for (var i = 0 to 10 step 2) {
    // Do something
}
```

Abstract definition: for (var i: T = INIT to MAX step STEP) {}
* i and INIT are of type T
* T must support the following operators:
1. ==(typeof(MAX)): possible to compare it with MAX
2. +(typeof(STEP)) -> possible to add STEP to it

However, no foreach loop will be added because it would require to ship some types by default (like Optional).
# Feature plan

See [example file](examples/newfeatures.si) for descriptions of features.

This file aims to already think about the implementation

## Generic types

### Low level representation once compiled

Each type/function created with generic parameters is produced once for each combination.

### Implementation idea

- Add a `GenericType` in the `types` directory
- A generic type stores a list of function definitions that must be supported (name, return type, parameter types in what order)
- functions, classes, and enums are modified to support generic types
- when compiling, keep track of what "implementations" were already created (if `Optional<Int>` is used several times, only produce it once)
- when registering a new implementation, each "substitute type" is checked for supported functions. An error is thrown if a type isn't fitting

## Classes and function namespacing

- each type will be able to hold functions

## Operator overloading

### Low level representation once compiled

- operators for primitive types are inlined (e.g a + remains a +)
- operators for reference types (classes) are functions that are implicitly called

### Implementation idea

- Update the function type and add a `isOperator` getter that returns true if the function is an operator
- Operator functions can be created by code or manually in java
- An operator can be flagged as commutative (boolean)
- java version will be used to define operators on primitives (hardcoded operators)
- when checking for binary or unary operators, the check will be modified: now, the operator is looked up in the left operand's type with the type of the right operand. If nothing is found, the right operand's type is also checked, but only for operations that are flagged as commutative.

Interesting notes:
- Constructor is also an operator. By default, it takes the properties of the class in order for parameters, and affects them respectively (like in the vanilla version of sigh)
- Conversion to string is also an operator. Maybe a syntax can be decided for it, so that we are not obliged to do `print("" + x)` to print x ? Or maybe make it implicit, like cast<T> which is called when x of another type is used at a place where we would expect T, that way it works for string but can also be used elsewhere (e.g convert a fraction to float implicitly)

## Enums

### Low level representation once compiled

- Each variant is mapped to an integer
- The required size to store it will be the smallest multiple of 8 bits that can hold the biggest number

```rust
enum Order{
    FIRST,  // -> 0
    SECOND, // -> 1
    THIRD   // -> 2
}
// -> sizeof(Order) == 1
```

- When data is stored in variants, it is stored after the id in memory
- The allocated space must be large enough to hold the largest possible value
- However, only one value is stored at a time, so no need to allocate multiple values. Only the largest one.

```rust
enum Optional{
    VALUE(Int),  // -> 0
    NOTHING,     // -> 1
}
// -> sizeof(Order) == 5
```

### Implementation idea

- Add `EnumType` in the `types` directory
- ...

## Type inference

- Analyse the sub-expressions first, then store the type of the current expression
- Idem for affectation

## Pattern matching

- If the lvalue of an declaration is not a reference (e.g we do not simply affect a variable but have a more complex expression):
    - Check that the pattern is valid. Only the following are supported:
    - enum variants, literals
    - there must be at least one reference that does not already exist in this scope (variable to create affect) -> otherwise, it is not a declaration !
    - but some literals are permitted

example: `var ["hello", x] = rvalue` will match if the right expression is a list with 2 items, where the first one is "hello". It is the equivalent of:

```
if (rvalue.length() = 2 && rvalue[0] == "hello") {
    var x = rvalue[1]
    // ...
}
```

No operators are permitted, though.

- The affectation is now a special expression, and returns a boolean
- If the lvalue is a pattern, the affectation MUST be used as a single-level condition in a if or while condition
- The result of the affectation CANNOT be stored in a variable

```
var x = (var ["hello", y] = rvalue) // NO, it's weird
```

- The result of the affectation CANNOT be operated on

```
if (!(var ["hello", y] = rvalue)) { // NO, it's weird, and useless
    // ...
}
```

## Switch

simply transform it in an IF with various branches

## Keywords

- typeof
- sizeof

replace them with constant on compilation

example: `typeof(2)` is replaced by a constant `Int`

## For loop

Transform it in a while

```
for (var i = 0 to 10) {
    // Do something
}
```

becomes

```
{
    var i = 0
    while (i <= 10) {
        
        // Do something

        i += 1
    }
}
```

- Note: warning, the for is it's own scope ! `i` must not be accessible outside of it, which would be the case by simply replacing it with an unscoped while
- Use the flexibility of the operator system to allow the usage of any type in the for, as long as it supports the operations

## Summary

- Revamp of type system to allow functions to be associated with types and generics
- Some functions can be operators. Default operators are defined for primitive types
- Existing code is ported to the new operator checking. It will take a bit of time, but shouldn't take that long. After it, the system will be very generic and extensible
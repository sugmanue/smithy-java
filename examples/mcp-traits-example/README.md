# MCP Traits Example

This example demonstrates how to use the `@prompts` trait from the `mcp-traits` module to provide LLM guidance for Smithy services. The `@prompts` trait allows you to annotate services, resources, and operations with prompt templates that help LLMs understand when and how to use your API.

## Overview

The `@prompts` trait enables you to:

- **Provide contextual guidance**: Help LLMs understand when to use specific operations
- **Include parameter descriptions**: Guide LLMs on how to populate operation inputs
- **Define usage conditions**: Specify when certain operations should be preferred
- **Create comprehensive templates**: Provide detailed instructions and examples

## Example Implementation

This example implements a simple `UserService` that demonstrates the key features of the `@prompts` trait:

### Service-Level Prompts

```smithy
@prompts({
    search_users: { 
        description: "Search for users in the system by various criteria", 
        template: "Search for users where {{searchCriteria}}. Use pagination with limit={{limit}} if many results expected.", 
        arguments: SearchUsersInput, 
        preferWhen: "User wants to find specific users or browse user lists" 
    }
})
service UserService {
    operations: [
        SearchUsers
    ]
}
```

### Key Features

1. **Service-Level Guidance**: The service includes a prompt that explains how to search for users
2. **Parameter Templates**: Uses `{{searchCriteria}}` and `{{limit}}` placeholders that reference the input structure
3. **ArgumentShape Integration**: References `SearchUsersInput` to provide parameter context
4. **Conditional Usage**: Uses `preferWhen` to specify when this operation should be preferred

### Complete Model Structure

```smithy
namespace com.example

use smithy.ai#prompts

@prompts({
    search_users: { 
        description: "Search for users in the system by various criteria", 
        template: "Search for users where {{searchCriteria}}. Use pagination with limit={{limit}} if many results expected.", 
        arguments: SearchUsersInput, 
        preferWhen: "User wants to find specific users or browse user lists" 
    }
})
service UserService {
    operations: [
        SearchUsers
    ]
}

operation SearchUsers {
    input: SearchUsersInput
    output: SearchUsersOutput
}

structure SearchUsersInput {
    searchCriteria: String
    limit: Integer
}

structure SearchUsersOutput {
    id: String
}
```

## Prompt Template Structure

Each prompt template in the `@prompts` trait map has the following structure:

```smithy
"prompt_name": {
    description: String        // Required: Brief description of the prompt's purpose
    template: String          // Required: The actual prompt template with placeholders
    arguments: ArgumentShape  // Optional: Reference to structure defining parameters
    preferWhen: String       // Optional: Condition describing when to use this prompt
}
```

### Template Placeholders

Use `{{parameterName}}` syntax in templates to reference parameters defined in the `arguments` structure:

```smithy
template: "Search for users where {{searchCriteria}}. Use pagination with limit={{limit}} if many results expected."
arguments: SearchUsersInput
```

The placeholders `{{searchCriteria}}` and `{{limit}}` correspond to the fields in the `SearchUsersInput` structure.

### Conditional Usage

The `preferWhen` field helps LLMs decide when to use specific operations:

```smithy
preferWhen: "User wants to find specific users or browse user lists"
```

## Building and Running

### Prerequisites

- Java 17 or later
- Gradle 7.0 or later

### Build the Example

```bash
# From the example directory
gradle build
```

### Validate the Model

```bash
# Run Smithy build to validate the model and traits
gradle smithyBuild
```

### Expected Output

When you run `gradle smithyBuild`, you should see:

1. **Successful build** - indicating all traits are applied correctly
2. **No validation errors** - confirming the model structure is valid
3. **Generated build artifacts** - in the `build/smithyprojections/` directory

## Usage Patterns

### 1. Service-Level Guidance

Provide high-level guidance about the entire service:

```smithy
@prompts({
    service_overview: {
        description: "Overview of the user management service"
        template: "This service manages user accounts and provides search capabilities."
        preferWhen: "User needs general information about user management"
    }
})
service UserService {
    // ...
}
```

### 2. Parameter-Rich Templates

Create detailed templates that guide parameter usage:

```smithy
template: "Search for users where {{searchCriteria}}. Use pagination with limit={{limit}} if many results expected."
```

### 3. Contextual Guidance

Use `preferWhen` to provide context about when operations should be used:

```smithy
preferWhen: "User wants to find specific users or browse user lists"
```

## Best Practices

### 1. Clear Descriptions

- Use concise but descriptive prompt names
- Write clear `description` fields that explain the prompt's purpose
- Make templates self-contained and easy to understand

### 2. Comprehensive Templates

- Include all relevant parameters in templates using `{{parameterName}}` syntax
- Provide examples and usage patterns within templates
- Explain constraints and expected input formats

### 3. Meaningful Parameter References

- Use the `arguments` field to reference input structures
- Ensure parameter names in templates match structure field names
- Keep parameter names descriptive and intuitive

### 4. Conditional Logic

- Use `preferWhen` to guide operation selection
- Be specific about when operations should be used
- Consider user intent and context in conditions

## Integration with MCP

This example is designed to work with Model Context Protocol (MCP) servers. The `@prompts` trait provides the metadata needed for MCP servers to:

1. **Understand API capabilities** - through service-level prompts
2. **Generate appropriate calls** - using operation-specific guidance
3. **Populate parameters correctly** - through ArgumentShape references
4. **Provide context-aware suggestions** - using `preferWhen` conditions

## Extending the Example

To extend this example, you could:

1. **Add more operations** with their own prompt templates
2. **Include resource-level prompts** for hierarchical guidance
3. **Add error handling prompts** for common failure scenarios
4. **Create workflow prompts** that guide multi-step processes

## Further Reading

- [Smithy Trait Documentation](https://smithy.io/2.0/spec/traits.html)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [Smithy Java Documentation](https://smithy.io/2.0/languages/java/)

## License

This example is provided under the same license as the Smithy Java project.

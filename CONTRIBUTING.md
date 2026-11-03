# Contributing to SunLink

We welcome contributions to the SunLink project! This document provides guidelines for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Documentation](#documentation)

## Code of Conduct

Please read and follow our [Code of Conduct](CODE_OF_CONDUCT.md) to ensure a positive and inclusive environment for all contributors.

## Getting Started

### Prerequisites

- Python 3.11+
- Docker and Docker Compose
- Git

### Setup

1. Fork the repository
2. Clone your fork locally:
   ```bash
   git clone https://github.com/your-username/sunlink.git
   cd sunlink
   ```
3. Create a virtual environment:
   ```bash
   python -m venv venv
   source venv/bin/activate
   ```
4. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
5. Set up environment variables:
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

## Development Workflow

### 1. Create a Branch

Create a new branch for your feature or bug fix:

```bash
git checkout -b feature/your-feature-name
```

### 2. Make Changes

Make your changes following the [Coding Standards](#coding-standards).

### 3. Test Your Changes

Run the test suite:

```bash
pytest tests/
```

### 4. Commit Your Changes

Commit your changes with a descriptive message:

```bash
git add .
git commit -m "Add feature: your feature description"
```

### 5. Push and Create Pull Request

Push your changes to your fork and create a pull request:

```bash
git push origin feature/your-feature-name
```

## Pull Request Process

### Pull Request Guidelines

1. **Title**: Use a clear and descriptive title
2. **Description**: Provide a detailed description of the changes
3. **Testing**: Describe how you tested your changes
4. **Documentation**: Include any necessary documentation updates
5. **Checklist**: Complete the pull request checklist

### Pull Request Checklist

- [ ] Code follows the [Coding Standards](#coding-standards)
- [ ] Tests pass and are updated if necessary
- [ ] Documentation is updated if needed
- [ ] The PR describes the changes made
- [ ] The PR has been tested on the target environment

## Coding Standards

### Python Code

We follow PEP 8 and use the following tools for code quality:

- **Black**: Code formatting
- **isort**: Import sorting
- **flake8**: Linting
- **mypy**: Type checking

### Running Code Quality Tools

```bash
# Format code
black .

# Sort imports
isort .

# Lint code
flake8 .

# Type check
mypy .
```

### Code Style Guidelines

1. **Use meaningful variable and function names**
2. **Include docstrings for all public functions and classes**
3. **Follow the existing code style**
4. **Write clean, readable code**
5. **Add comments for complex logic**

### Example

```python
def calculate_power(voltage: float, current: float) -> float:
    """
    Calculate electrical power.
    
    Args:
        voltage: Voltage in volts
        current: Current in amperes
        
    Returns:
        Power in watts
    """
    return voltage * current
```

## Testing

### Test Structure

- Unit tests in `tests/unit/`
- Integration tests in `tests/integration/`
- End-to-end tests in `tests/e2e/`

### Running Tests

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=app

# Run specific test file
pytest tests/unit/test_api.py

# Run specific test
pytest tests/unit/test_api.py::test_login
```

### Writing Tests

- Use pytest for testing
- Follow the Arrange-Act-Assert pattern
- Mock external dependencies
- Test both success and error cases

### Example Test

```python
def test_login_success(client):
    """Test successful login."""
    response = client.post("/api/v1/auth/login", json={
        "username": "admin",
        "password": "password"
    })
    assert response.status_code == 200
    assert "access_token" in response.json()
```

## Documentation

### Documentation Standards

1. **Keep documentation up to date**
2. **Use clear and concise language**
3. **Include examples where helpful**
4. **Document all public APIs**

### Documentation Types

- **README.md**: Project overview and quick start
- **API Documentation**: Available at `/docs` when running the server
- **Code Documentation**: Docstrings in the code
- **User Guides**: In the `docs/` directory

### Updating Documentation

When making changes, update the relevant documentation:

- API changes: Update API documentation
- New features: Add user guides
- Configuration changes: Update configuration documentation

## Reporting Issues

When reporting issues, please include:

1. **Bug description**: Clear and concise description
2. **Steps to reproduce**: Steps to reproduce the issue
3. **Expected behavior**: What should happen
4. **Actual behavior**: What actually happens
5. **Environment**: System and software versions
6. **Logs**: Relevant error logs

## Feature Requests

We welcome feature requests! Please include:

1. **Problem description**: What problem the feature solves
2. **Proposed solution**: How the feature should work
3. **Use cases**: How users would use this feature
4. **Alternatives**: Any alternative solutions considered

## Community

Join our community:

- **GitHub Discussions**: [Link to discussions]
- **Issues**: [Link to issues]
- **Discord**: [Link to Discord server]

## License

By contributing to SunLink, you agree that your contributions will be licensed under the MIT License.

Thank you for contributing to SunLink! 🚀
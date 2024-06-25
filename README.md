# Process Inspector

[![CI Build](https://github.com/axonivy-market/process-inspector/actions/workflows/ci.yml/badge.svg)](https://github.com/axonivy-market/process-inspector/actions/workflows/ci.yml)

Use this tool to analyze your process model and calculate the expected case completion time.

- Configure needed information directly in the process model
	- Default duration of a task for multiple use cases. Each task can have multiple named default durations.
	- Different "happy path" flows. It`s possible to set multiple named process paths.
- Possibilities to override settings of the process model
	- Override duration
	- Override default path for the gateways
- Create a list of all tasks in the process.
- Get configured duration for a task.
- Get all upcoming tasks on a configured process path with expected start timestamp for each task.

Read our [documentation](process-inspector-product/README.md).

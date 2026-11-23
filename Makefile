.PHONY: check lint test gametest

# Everything CI runs that works headless: Python lint, compile, unit tests, jar build, server GameTests.
# Client GameTests need a display and sound device, so CI runs them separately (see .github/workflows/build.yml).
check: lint
	sh ./gradlew build runGameTest

lint:
	ruff check tools

test:
	sh ./gradlew test

gametest:
	sh ./gradlew runGameTest

---
allowed-tools: bash(mvn clean package:*)
description: builds the application
---

Run `mvn clean package`
If $ARGUMENTS says to skip tests: `mvn clean package -DskipTests`

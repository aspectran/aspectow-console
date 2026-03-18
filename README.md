# Aspectow Management Console

[![Build Status](https://github.com/aspectran/aspectow-console/workflows/Java%20CI/badge.svg)](https://github.com/aspectran/aspectow-console/actions?query=workflow%3A%22Java+CI%22)
[![Maven Central Version](https://img.shields.io/maven-central/v/com.aspectran/aspectow-console)](https://central.sonatype.com/artifact/com.aspectran/aspectow-console)
[![License](https://img.shields.io/:license-apache-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

Aspectow Management Console is a central control system for Aspectow-based applications, providing integrated **Observability**, **Controllability**, and **Governance**. While AppMon focuses on real-time monitoring, the Management Console is designed to directly manage framework assets and securely handle sensitive resources.

## Key Vision

- **Integrated Management**: Combines monitoring (AppMon), framework introspection (Anatomy), and security asset (PBE) management into a single console.
- **Real-time Control**: Provides real-time diagnosis and configuration control for running applications.
- **Enterprise-Ready**: Supports clustering environments and ensures operational stability through strict Role-Based Access Control (RBAC).

## Features

- **Security & Admin**: RBAC-based user management and a secure Vault for PBE Token management.
- **Framework Introspection**: Visualize Translets, Beans, Aspects, and Schedules. Live Bean Explorer for real-time state diagnosis.
- **Operations & Monitoring**: Integrated AppMon dashboard for logs and metrics, plus dynamic configuration and scheduler control.

## Requirements

- Java 21 or later
- Maven 3.9.4 or later (the included Maven wrapper is recommended)

## Building from Source

To build Aspectow Management Console from the source code, follow these steps:

1.  **Clone the repository:**
    ```sh
    git clone https://github.com/aspectran/aspectow-console.git
    ```

2.  **Navigate to the project directory:**
    ```sh
    cd aspectow-console
    ```

3.  **Build the project:**
    ```sh
    ./build rebuild
    ```

## Running the Demo

After a successful build, you can run the included demo application.

1.  **Start the demo server:**
    ```sh
    ./build demo
    ```

2.  **Access the console:**
    Open your web browser and navigate to [http://localhost:8082/console](http://localhost:8082/console).

## License

Aspectow Management Console is licensed under the [Apache License 2.0](LICENSE.txt).

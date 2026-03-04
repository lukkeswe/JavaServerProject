# Java Web Server

A lightweight HTTP web server written in pure Java.

Built from scratch using Java’s built-in HTTP capabilities — without servlets, frameworks, or external web libraries.

## Features

- Static file serving (HTML, CSS, JS, images)
- PHP execution via `php-cgi`
- Host-based routing
- Basic MySQL integration
- Linux service support (systemd)

## Technologies

- Java
- PHP (php-cgi)
- MySQL
- Linux

## Run

### Compile
```bash
javac -cp .:mysql-conn.jar Main.java

### Start
```bash
java -cp .:mysql-conn.jar Main

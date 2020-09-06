# net.frontuari.bpallocation

- Copyright: 2020 Frontuari, C.A. <http://frontuari.net>
- Repository: https://github.com/frontuari/net.frontuari.bpallocation
- License: GPL 2

## Description

This plugins can allocate documents (Invoices and Payments) between two difference business partners.

## Contributors

- Jorge Colmenarez - Frontuari, C.A. <jcolmenarez@frontuari.net>.
- Ing. Victor Suarez <victor.suarez.is@gmail.com>

## Components

- iDempiere Plugin [net.frontuari.bpallocation](net.frontuari.bpallocation)
- iDempiere Unit Test Fragment [net.frontuari.bpallocation.test](net.frontuari.bpallocation.test)
- iDempiere Target Platform [net.frontuari.bpallocation.targetplatform](net.frontuari.bpallocation.targetplatform)

## Prerequisites

- Java 11, commands `java` and `javac`.
- iDempiere 7.1.0
- Set `IDEMPIERE_REPOSITORY` env variable

## Features/Documentation

### Mejora en validacion de "Fecha erronea de Asignacion"
- Se extiende Funcionalidad de Asignaciones de iDempiere, mejorando su funcionamiento, permitiendo mediante el configurador de sistema **VS_DaysDiffPermittedForAllocation**, establecer una cantidad de dias de holgura para que la asignacion no arroje error cuando es de fecha menor a los Documentos de Factura y/o Pagos de las lineas.

## Instructions

### Mejora en validacion de "Fecha erronea de Asignacion"
- Instalar Plugin
- Verificar si se instalo el configurador **VS_DaysDiffPermittedForAllocation**
- Copiar dicho configurador a su Grupo Empresarial
- Colocar el valor numerico deseado en el configurador **VS_DaysDiffPermittedForAllocation**, deben ser valores negativos, si queremos que la asignacion pueda tener fechas anteriores a los Documentos de Facturas y Pagos de la Asignacion.
- Si el configurador no existe, el valor por defecto es 0, por lo que tomara el comportamiento original.

## Extra Links

- Put the documentation/links here

## Commands

Compile plugin and run tests:

```bash
./build
```

Use the parameter `debug` for debug mode example:

```bash
./build debug
```

To use `.\build.bat` for windows.

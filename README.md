## YAFI - Yet Another Fault Injector Implementation

YAFI is a fault injector tool that allows you to test the reliability of your system by injecting faults and evaluating the response of the system to those faults. The tool is designed to work with a single client (the orchestrator) and multiple servers (nodes), capable of running failure plans.

This implementation is made to work with Java applications and has been tested on Linux.
Extending it to other languages requires the creation of a new *injector*.

Some language-agnostic features are offered through SSH commands.
See *Connections* and *Capabilities->Fault type*: **network manipulation** for more information.

### Architecture

The YAFI tool is composed of an orchestrator and multiple agents. The orchestrator sends commands to the agents to inject faults into the system under test. The agents execute these commands and report back to the orchestrator with the results. The orchestrator then compares the results of the injected faults with a reference run to evaluate the reliability of the system.

This implementation works by setting up a socket between the orchestrator and the agents.
The orchestrator sends commands to the agents, which they handle accordingly.
Both sides must be implemented according to the same contract for proper effect.
See *Injectors* for example use cases.

### Installation

Depending on the faults you wish to inject, there are different additional requirements.

For the application itself the only requirement is:
- Java 11+

While the orchestrator and agent are usable on any platform, they have only been tested on:
- Ubuntu 22.04
- MacOs Monterey
- Windows* ** ***

*Keep in mind that Windows does not natively support some bash and other operations. Metric (cpu load) gathering will fail.\
**Symlinks require special privilege, `log/latest` creation will not work by default.\
***Agents were mainly run on WSL2.0.

Download the repo and create the directories where you will store execution plans.
On the orchestrator `/generatedplans` and `/log`: are created in the repo dir automatically.

On the target nodes, we require a directory for storing plans.
Create the directory that you wish to use and make sure to use the created dir in `-Ddatadir=[YOUR_PATH]`.

then run 
```
./gradlew jar
```

### Usage

To use YAFI, you will need to implement a functional test by creating a workflow in combination with a script that runs the system under test. The tool works by running a reference run, gathering information in the form of tracing or logging data, and parsing that data into actionable failure plans. These failure plans are then executed as experiments, and the results are compared to the reference run.

To set up YAFI, you will need to:

1. Define failure plans in YAML for each node in the system.
2. Start the agents on each node.
3. Run a reference run to gather data for parsing into failure plans.
4. Define experiments to run using the parsed failure plans.
5. Run the experiments and compare the results to the reference run.

### Failure Plan

The failure plan is the template to follow for an experiment.
It can contain multiple faults in a single plan which will trigger at their trigger point.
In the failure plan, we also define target nodes (hosts).



### Connections

A connection is the link between the text execution node (this is where the orchestrator runs and can be your local machine) and a target node.
These facilitate sending commands and information both ways.
A command can be defined by any injector and is subject to certain formatting.



### Injectors

The injectors are responsible for enabling faults on the target system.

#### JVM Chaos Injector
In this proof of concept, we have implemented JVM chaos injection as way to modify the SUT.
It works by leveraging [Byteman](https://byteman.jboss.org/), manipulating the SUT at runtime with custom rules.

For this injector, a translation needs to take place from the failure plan to the injector-format.
In the case of Byteman, this is a ".btm" file, containing rules conforming to the [ECA rule format](https://github.com/bytemanproject/byteman/blob/main/docs/asciidoc/src/main/asciidoc/chapters/Byteman-Rule-Language.adoc).
The translation is done by the `nl.freshcoders.fit.plan.parser.TraceInstaller`. (though the "trace" part is not apt naming)


### Capabilities

Here we list some capabilities with the injector that offer them, as well as the connection that is required. 

| Faults              | Injectors          | Connector |
|---------------------|--------------------|-----------|
| method delay        | JVM Chaos Injector | Socket    |
| exception           | JVM Chaos Injector | Socket    |
| clock               | JVM Chaos Injector | Socket    |
| network manipulation| NetEm Injector     | SSH       |

#### Fault Types

Note that there are multiple ways to achieve the same faults.
These are current implementations of this PoC.

- method delay: injected through code manipulation as a BTM rule.
- exception: injected through code manipulation as a BTM rule.
- clock: injected through code manipulation as a BTM rule.
- network manipulation: facilitated by `tc` and `NetEm`, directly called from an SSH connection.
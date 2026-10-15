# Multi-Agent Coordination for Autonomous Graph Exploration and Target Encirclement

An autonomous, decentralized Multi-Agent System (MAS) built on the **Dedale** platform (JADE framework) for distributed mapping and coordinated mobile target capture in unknown dynamic topologies.

* **Author:** Thomas LU
* **Institution:** Sorbonne University (M.Sc. in Computer Science / Robotics & AI — LIP6)
* **Framework:** Java, JADE, Dedale Platform, GraphStream

---

## 🎯 Overview

This project implements a distributed fleet of autonomous software agents tasked with:
1. **Decentralized Topology Exploration:** Incrementally discovering, mapping, and synchronizing a shared graph of an unknown environment without a central server.
2. **Coordinated Encirclement (Golem Hunting):** Detecting olfactory traces, forming dynamic coalitions via distributed flood protocols, and simultaneously blocking all adjacent escape routes around mobile entities.

---

## 🏗️ System Architecture

The architecture enforces strict decoupling between spatial knowledge, decision-making logic, and inter-agent communication protocols:

### 1. Agent Abstraction (`AbstractCommunicationAgent`)
* Base class encapsulating mailbox management, asynchronous communication lifecycles, and localized spatial belief states.
* Extensible hierarchy enabling specialized agent profiles (e.g., deterministic sentinels for protocol testing vs. active scouts).

### 2. Spatial Belief & Navigation (`MapRepresentation`)
* Internal graph structure storing incremental topology along with semantic metadata (`NodeInfo`).
* **Weighted Dijkstra Pathfinding:** Real-time edge cost modulation penalizing allied agent presence, preventing bottlenecks, and naturally producing an emergent pincer/encirclement movement.
* **Hierarchical Exploration Policy:** 
  1. *High Priority:* Navigating to frontier/open nodes.
  2. *Medium Priority:* Refreshing stale nodes where `lastUpdate > 20s` to track mobile entities in previously mapped zones.
  3. *Low Priority:* Stochastic anti-deadlock fallback.

### 3. Differential Communication Protocol (`Ping-Pong-ACK`)
* **Bandwidth Optimization:** Avoids transmitting the complete map. Agents store timestamped interaction history (`HashMap<AgentID, Long>`) and use `createSubGraph` to serialize and transfer only nodes modified since their last encounter.
* **Spatiotemporal Conflict Resolution (`mergeMap`):** Merges incoming subgraphs using strict timestamp supremacy (`lastUpdate`) and state-priority guards (preventing obsolete information from reopening closed nodes).
* **Piggybacking:** Ambient olfactory readings (`smellGlobalEstimation`) are attached to discovery pings to disseminate strategic clues with zero network overhead.

### 4. Coalition Formation & Target Encirclement (`coalition_golem`)
* Modeled as a **Finite State Machine (FSM)** with specialized roles:
  * **Leader (`CoalitionLeaderGolemBehaviour`):** Initiates recruitment, collects response echoes, computes assignment slots for target-adjacent nodes, and dispatches move directives.
  * **Participant (`CoalitionParticipantGolemBehaviour`):** Relays discovery signals, reports subtree availability, and moves to assigned blocking coordinates.
* **Optimized Flooding:** Employs unique `conversation_id` filtering to immediately suppress redundant messages and stop broadcast storms.
* **Leadership Conflict Arbitration:** Priority-based delegation rules ensure stable command hierarchies when multiple agents spot the target simultaneously.

---

## 📊 Algorithmic Complexity & Properties

| Component | Time Complexity (T) | Communication Complexity (C) | Optimality & Termination |
| :--- | :--- | :--- | :--- |
| **Exploration** | $\mathcal{O}(V \log V + E)$ | $\mathcal{O}(N)$ per cycle | Complete (all reachable open nodes resolved) |
| **Encirclement** | $\mathcal{O}(N_{agents} \times \text{Dijkstra})$ | $\mathcal{O}(N)$ (optimized flood) | Heuristic pincer; terminates when target is fully blocked |
| **Map Sync** | $\mathcal{O}(E)$ (preprocessing) | $\mathcal{O}(V_{diff} + E_{diff})$ | Optimal differential update verified by `ACK` |

---

## 📂 Repository Structure

```text
.
├── src/
│   └── eu/su/mas/dedaleEtu/
│       ├── mas/
│       │   ├── agents/          # Agent implementations (AbstractCommunicationAgent, etc.)
│       │   └── behaviours/      # Core behaviours (Listener, MainHunting, Ping, SendMsg)
│       ├── princ/               # Main simulation launcher and platform bootstrapping
│       └── sdm/
│           ├── coalition_golem/ # FSM implementations for hunting coalitions
│           └── map/             # MapRepresentation, NodeInfo, and weighted Dijkstra engine
├── doc/                         # Technical architecture documentation and UML diagrams
├── pom.xml                      # Maven build configuration
└── README.md

## 🚀 Build & Execution

### Prerequisites
* **Java Development Kit (JDK):** Version 8 or 11
* **Maven:** Version 3.6+
* **Dedale / JADE runtime libraries**

### Build
```bash
git clone [https://gitlab.com/thomaslu1245/dedale-etu-thomas-lu.git](https://gitlab.com/thomaslu1245/dedale-etu-thomas-lu.git)
cd dedale-etu-thomas-lu
mvn clean install

### Run Simulation
Launch the platform entry point directly via Maven:

```bash
mvn exec:java -Dexec.mainClass="eu.su.mas.dedaleEtu.princ.Principal"

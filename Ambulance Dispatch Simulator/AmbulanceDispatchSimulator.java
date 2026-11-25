import java.util.*;

public class AmbulanceDispatchSimulator {

    // ---------- Edge: one road to a neighbour location ----------
    static class Edge {
        String to;   // neighbour location
        int km;      // distance in km

        Edge(String to, int km) {
            this.to = to;
            this.km = km;
        }
    }

    // ---------- Node: item stored in Dijkstra's PriorityQueue ----------
    // The node with the smallest distance comes out first.
    static class Node implements Comparable<Node> {
        String name;
        int dist;

        Node(String name, int dist) {
            this.name = name;
            this.dist = dist;
        }

        public int compareTo(Node other) {
            return this.dist - other.dist;
        }
    }

    // ---------- Graph: the city map (adjacency list) ----------
    static class Graph {
        HashMap<String, ArrayList<Edge>> adj = new HashMap<>();
        HashMap<String, Integer> dist = new HashMap<>();   // shortest distance found
        HashMap<String, String> parent = new HashMap<>();  // previous location on the path

        // A road works in both directions, so add it twice.
        void addRoad(String a, String b, int km) {
            if (!adj.containsKey(a)) adj.put(a, new ArrayList<Edge>());
            if (!adj.containsKey(b)) adj.put(b, new ArrayList<Edge>());
            adj.get(a).add(new Edge(b, km));
            adj.get(b).add(new Edge(a, km));
        }

        boolean hasLocation(String name) {
            return adj.containsKey(name);
        }

        // Dijkstra's algorithm: finds the shortest distance from src to every location.
        void dijkstra(String src) {
            dist.clear();
            parent.clear();
            for (String loc : adj.keySet()) {
                dist.put(loc, 999999);          // 999999 means "not reached yet"
            }
            dist.put(src, 0);

            PriorityQueue<Node> pq = new PriorityQueue<>();
            pq.add(new Node(src, 0));

            while (!pq.isEmpty()) {
                Node current = pq.poll();       // closest location not yet processed
                if (current.dist > dist.get(current.name)) {
                    continue;                   // old entry, a shorter way was already found
                }
                for (Edge e : adj.get(current.name)) {
                    int newDist = current.dist + e.km;
                    if (newDist < dist.get(e.to)) {      // shorter way found
                        dist.put(e.to, newDist);
                        parent.put(e.to, current.name);
                        pq.add(new Node(e.to, newDist));
                    }
                }
            }
        }

        int getDistance(String dest) {
            return dist.get(dest);
        }

        // Builds the path text by going back from dest to the source using parent.
        String getPath(String dest) {
            String path = dest;
            String p = parent.get(dest);
            while (p != null) {
                path = p + " -> " + path;
                p = parent.get(p);
            }
            return path;
        }
    }

    // ---------- Ambulance: id, current location and status ----------
    static class Ambulance {
        String id;
        String location;
        boolean available;

        Ambulance(String id, String location) {
            this.id = id;
            this.location = location;
            this.available = true;
        }
    }

    // ---------- Emergency: location, severity and arrival number ----------
    static class Emergency implements Comparable<Emergency> {
        String location;
        int severity;   // 1 = critical, 3 = minor
        int seq;        // arrival number

        Emergency(String location, int severity, int seq) {
            this.location = location;
            this.severity = severity;
            this.seq = seq;
        }

        // Lower severity number first; if equal, the earlier request first.
        public int compareTo(Emergency other) {
            if (this.severity != other.severity) {
                return this.severity - other.severity;
            }
            return this.seq - other.seq;
        }
    }

    // ---------- Simulator data ----------
    Graph graph = new Graph();
    ArrayList<Ambulance> ambulances = new ArrayList<>();
    PriorityQueue<Emergency> waiting = new PriorityQueue<>();
    int arrivalCounter = 0;

    AmbulanceDispatchSimulator() {
        graph.addRoad("Hospital", "Sector1", 4);
        graph.addRoad("Sector1", "Sector2", 3);
        graph.addRoad("Sector2", "Sector3", 5);
        graph.addRoad("Hospital", "Sector3", 10);
        ambulances.add(new Ambulance("AMB1", "Hospital"));
        ambulances.add(new Ambulance("AMB2", "Sector3"));
    }

    // Dispatch: send the nearest free ambulance, or put the request in the queue.
    void dispatch(String loc, int sev) {
        if (!graph.hasLocation(loc)) {
            System.out.println("Location " + loc + " not found.");
            return;
        }
        boolean sent = sendNearest(loc);
        if (!sent) {
            arrivalCounter++;
            waiting.add(new Emergency(loc, sev, arrivalCounter));
            System.out.println("No ambulance available. Request queued.");
        }
    }

    // Runs Dijkstra from every free ambulance and sends the one with the smallest distance.
    // Returns false if there is no free ambulance.
    boolean sendNearest(String loc) {
        Ambulance best = null;
        int bestDist = 999999;
        String bestPath = "";

        for (Ambulance a : ambulances) {
            if (a.available) {
                graph.dijkstra(a.location);
                int d = graph.getDistance(loc);
                if (d < bestDist) {              // '<' keeps the first ambulance on a tie
                    best = a;
                    bestDist = d;
                    bestPath = graph.getPath(loc);
                }
            }
        }

        if (best == null) {
            return false;
        }

        best.available = false;
        best.location = loc;
        System.out.println("Nearest ambulance: " + best.id);
        System.out.println("Path: " + bestPath + " | Distance: " + bestDist + " km");
        System.out.println(best.id + " status: Busy");
        return true;
    }

    // Complete trip: free the ambulance, then serve the most severe waiting request.
    void completeTrip(String id) {
        Ambulance amb = null;
        for (Ambulance a : ambulances) {
            if (a.id.equals(id)) {
                amb = a;
            }
        }
        if (amb == null) {
            System.out.println("Ambulance " + id + " not found.");
            return;
        }
        if (amb.available) {
            System.out.println(amb.id + " is not on a trip.");
            return;
        }

        amb.available = true;
        if (waiting.isEmpty()) {
            System.out.println(amb.id + " is free at " + amb.location + ". No waiting requests.");
        } else {
            Emergency next = waiting.poll();      // most severe request
            System.out.println(amb.id + " is free. Serving most severe request: "
                    + next.location + " (Severity " + next.severity + ").");
            sendNearest(next.location);
        }
    }

    void showStatus() {
        System.out.println("ID      Status      Location");
        for (Ambulance a : ambulances) {
            String status = a.available ? "Available" : "Busy";
            System.out.println(a.id + "    " + status + "   " + a.location);
        }
        System.out.println("Waiting requests: " + waiting.size());
    }

    // Reads a whole number between min and max; asks again if the input is wrong.
    static int readNumber(Scanner sc, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String text = sc.next();
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // not a number, fall through to the message below
            }
            System.out.println("Invalid input. Enter a number from " + min + " to " + max + ".");
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        AmbulanceDispatchSimulator sim = new AmbulanceDispatchSimulator();
        int choice = 0;

        while (choice != 4) {
            System.out.println("\n===== AMBULANCE DISPATCH SIMULATOR =====");
            System.out.println("1. Dispatch Emergency");
            System.out.println("2. Complete Trip");
            System.out.println("3. Show Status");
            System.out.println("4. Exit");
            choice = readNumber(sc, "Enter choice: ", 1, 4);

            if (choice == 1) {
                System.out.print("Emergency location: ");
                String loc = sc.next();
                int sev = readNumber(sc, "Severity (1 = critical, 3 = minor): ", 1, 3);
                sim.dispatch(loc, sev);
            } else if (choice == 2) {
                System.out.print("Ambulance ID (AMB1 or AMB2): ");
                String id = sc.next();
                sim.completeTrip(id);
            } else if (choice == 3) {
                sim.showStatus();
            }
        }
        System.out.println("Thank you. Program ended.");
        sc.close();
    }
}
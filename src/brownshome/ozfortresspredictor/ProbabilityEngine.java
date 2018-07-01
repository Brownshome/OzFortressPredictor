package brownshome.ozfortresspredictor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;

public class ProbabilityEngine {
	//The match state can be modelled as a markov machine
	//Each state has 0-2 incomming states, a P(0) and possibly outgoing conditions
	
	public static class Pair {
		public final int a, b;
		
		public Pair(int a, int b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public int hashCode() {
			return Objects.hash(a, b);
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			
			if(!(obj instanceof Pair))
				return false;
			
			Pair other = (Pair) obj;

			return other.a == a && other.b == b;
		}

		private Pair aEntrance() {
			return new Pair(a - 1, b);
		}
		
		private Pair bEntrance() {
			return new Pair(a, b - 1);
		}
	}
	
	public class State {
		private final double[] p = new double[steps + 1];
		public final int scoreA, scoreB;
		
		private State(Pair pair) {
			boolean exit;
			boolean entranceA;
			boolean entranceB;
			scoreA = pair.a;
			scoreB = pair.b;
			
			assert scoreA >= 0 && scoreB >= 0 && Math.abs(scoreA - scoreB) <= limit;
			
			if(scoreA == scoreB + limit) {
				//P(x+L:x)
				exit = false;
				entranceA = true;
				entranceB = false;
			} else if(scoreA + limit == scoreB) {
				//P(x:x+L)
				exit = false;
				entranceA = false;
				entranceB = true;
			} else if(scoreA == 0 && scoreB == 0) {
				//P(0:0)
				exit = true;
				entranceA = false;
				entranceB = false;
			} else if(scoreA == 0) {
				//P(0:x)
				exit = true;
				entranceA = false;
				entranceB = true;
			} else if(scoreB == 0) {
				//P(x:0)
				exit = true;
				entranceA = true;
				entranceB = false;
			} else if(scoreA == scoreB + limit - 1) {
				//P(x+L-1:x)
				exit = true;
				entranceA = true;
				entranceB = false;
			} else if(scoreA + limit - 1 == scoreB) {
				//P(x:x+L-1)
				exit = true;
				entranceA = false;
				entranceB = true;
			} else {
				//P(A:B)
				exit = true;
				entranceA = true;
				entranceB = true;
			}
			
			State aEntrance = null;
			State bEntrance = null;
			
			if(entranceA) aEntrance = state(pair.aEntrance());
			if(entranceB) bEntrance = state(pair.bEntrance());
			
			p[0] = scoreA == 0 && scoreB == 0 ? 1 : 0;
			
			double dt = 1.0 / steps;
			for(int i = 1; i <= steps; i++) {
				/*if(exit) {
					//P(A:B) = [(1 - dt(a+b)/2)P(A:B) + aP(A-1:B) + bP(A:B-1)] / (1 + dt(a+b)/2)
				} else {
					//P(A:B) = P(A:B) + aP(A-1:B) + bP(A:B-1)
				}*/
				
				p[i] = p[i - 1];
				
				if(exit)
					p[i] *= 1 - dt * (a + b) / 2;
				
				if(entranceA)
					p[i] += dt * a * (aEntrance.p[i] + aEntrance.p[i - 1]) / 2;
				
				if(entranceB)
					p[i] += dt * b * (bEntrance.p[i] + bEntrance.p[i - 1]) / 2;
				
				if(exit)
					p[i] /= 1 + dt * (a + b) / 2;
			}
			
			mapping.put(pair, this);
		}
		
		public double prob() {
			return p[steps];
		}
		
		@Override
		public String toString() {
			return "P(" + scoreA + ", " + scoreB + ") = " + ((int) (prob() * 1000)) / 10.0 + "%";
		}
	}
	
	private final Map<Pair, State> mapping = new HashMap<>();
	private final double a, b;
	private final int limit;
	private final int steps = 50;
	
	public ProbabilityEngine(double rateA, double rateB, int limit) {
		this.a = rateA;
		this.b = rateB;
		this.limit = limit;
	}
	
	private State state(Pair pair) {
		var result = mapping.get(pair);
		if(result == null) result = new State(pair);
		
		return result;
	}
	
	public double prob(int scoreA, int scoreB) {
		return state(new Pair(scoreA, scoreB)).prob();
	}
	
	public Queue<State> computeLikelyResults() {
		PriorityQueue<State> queue = new PriorityQueue<>((a, b) -> Double.compare(b.prob(), a.prob()));
		for(int a = 0; a < 100; a++) {
			for(int b = 0; b < 100; b++) {
				if(Math.abs(a - b) <= limit) {
					queue.add(state(new Pair(a, b)));
				}
			}
		}
		
		return queue;
	}
	
	public static void main(String[] args) throws IOException {
		ProbabilityEngine engine = new ProbabilityEngine(0, 2, 2);
		
		var queue = engine.computeLikelyResults();
		
		for(State next = queue.poll(); next.prob() >= 0.001; next = queue.poll()) {
			System.out.println(next);
		}
	}
}

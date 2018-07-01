package brownshome.ozfortresspredictor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

public final class Team {
	public class Result implements Comparable<Result> {
		public final Team other;
		public final int score, scoreOther, scoreLimit;

		private Result(Team other, int score, int scoreOther) {
			this.other = other;
			this.score = score;
			this.scoreOther = scoreOther;
			this.scoreLimit = 5;
		}

		public double probability() {
			var scoreRate = attack / other.defence;
			var otherScoreRate = other.attack / defence;
			
			return new ProbabilityEngine(scoreRate, otherScoreRate, scoreLimit).prob(score, scoreOther);
		}

		@Override
		public String toString() {
			return String.format("%s(%d) - %s(%d)", Team.this.name, score, other.name, scoreOther);
		}

		@Override
		public int compareTo(Result o) {
			return Double.compare(o.probability(), probability());
		}
	}

	public final String name;
	private final List<Result> matches = new ArrayList<>();

	public double attack, defence;
	private Team root;

	public Team(String name) {
		assert name != null;

		this.root = this;
		this.name = name;
	}

	private static final double timeAdjust = 0.0;
	public double probabilityOfResults() {
		double acc = 1.0;

		int i = 0;
		for(Result m : matches) {
			i++;
			acc *= Math.pow(m.probability(), 1.0 + i * timeAdjust);
		}

		return acc;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (obj instanceof Team && name.equals(((Team) obj).name));
	}

	public static void addScore(Team a, Team b, int aWins, int bWins) {
		var matchA = a.new Result(b, aWins, bWins);
		var matchB = b.new Result(a, bWins, aWins);

		a.matches.add(matchA);
		b.matches.add(matchB);

		mergeRoot(a, b);
	}

	public static List<Result> computeLikelyResults(Team a, Team b) {
		var scoreRate = a.attack / b.defence;
		var otherScoreRate = b.attack / a.defence;
		
		var engine = new ProbabilityEngine(scoreRate, otherScoreRate, 5);
		
		var queue = engine.computeLikelyResults();
		
		List<Result> list = new ArrayList<>();
		
		for(ProbabilityEngine.State next = queue.poll(); next.prob() > 0.001; next = queue.poll()) {
			list.add(a.new Result(b, next.scoreA, next.scoreB));
		}
		
		return list;
	}
	
	private static void mergeRoot(Team a, Team b) {
		a.collapseRoot();
		b.collapseRoot();
		a.root.root = b.root;
	}

	Team collapseRoot() {
		Team r = root;
		while(r.root != r) {
			r = r.root;
		}

		return root = r;
	}

	public double determineAttack() {
		double low = 0.0;
		double high = 1.0;

		while(high - low > 1e-6) {
			int sections = 4;
			double[] p = new double[sections + 1];

			int best = 0;
			double max = 0;
			for(int pi = 0; pi <= sections; pi++) {
				attack = p[pi] = (high * pi + low * (sections - pi)) / sections;
				double prob = probabilityOfResults();

				if(prob > max) {
					best = pi;
					max = prob;
				}
			}

			attack = p[best];

			if(best == 0) {
				high = p[1];
			} else if(best == sections) {
				low = p[sections - 1];
			} else {
				low = p[best - 1];
				high = p[best + 1];
			}
		}

		return probabilityOfResults();
	}

	public double determineDefence() {
		double low = 0.0;
		double high = 1.0;

		while(high - low > 1e-6) {
			int sections = 4;
			double[] p = new double[sections + 1];

			int best = 0;
			double max = 0;
			for(int pi = 0; pi <= sections; pi++) {
				defence = p[pi] = (high * pi + low * (sections - pi)) / sections;
				double prob = probabilityOfResults();

				if(prob > max) {
					best = pi;
					max = prob;
				}
			}

			defence = p[best];

			if(best == 0) {
				high = p[1];
			} else if(best == sections) {
				low = p[sections - 1];
			} else {
				low = p[best - 1];
				high = p[best + 1];
			}
		}

		return probabilityOfResults();
	}

	public double determineAttackAndDefence() {
		determineAttack();
		return determineDefence();
	}
}

package brownshome.ozfortresspredictor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.TreeSet;

public final class Team {
	private static ArrayList<BigInteger> factorial = new ArrayList<>();
	
	static {
		factorial.add(BigInteger.ONE);
	}
	
	public class Result implements Comparable<Result> {
		public final Team other;
		public final int score, scoreOther;

		private Result(Team other, int score, int scoreOther) {
			this.other = other;
			this.score = score;
			this.scoreOther = scoreOther;
		}

		public double probability() {
			var scoreRate = attack / other.defence;
			var otherScoreRate = other.attack / defence;
			return Math.pow(scoreRate, score) * Math.pow(otherScoreRate, scoreOther) * Math.exp(-scoreRate - otherScoreRate) / factorial(score).doubleValue() / factorial(scoreOther).doubleValue();
		}

		private BigInteger factorial(int i) {
			if(factorial.size() > i)
				return factorial.get(i);
			
			BigInteger ans = factorial(i - 1).multiply(BigInteger.valueOf(i));
			factorial.add(ans);
			return ans;
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
	private final Collection<Result> matches = new ArrayList<>();

	public double attack, defence;
	private Team root;

	public Team(String name) {
		assert name != null;

		this.root = this;
		this.name = name;
	}

	public double probabilityOfResults() {
		double acc = 1.0;

		for(Result m : matches) {
			acc *= m.probability();
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

	public static NavigableSet<Result> computeLikelyResults(Team a, Team b) {
		TreeSet<Result> results = new TreeSet<>();
		
		for(int aScore = 0; aScore <= 100; aScore++) {
			for(int bScore = 0; bScore <= 100; bScore++) {
				results.add(a.new Result(b, aScore, bScore));
			}
		}
		
		return results;
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

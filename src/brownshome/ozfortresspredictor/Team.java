package brownshome.ozfortresspredictor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Team {
	private static ArrayList<BigInteger> factorial = new ArrayList<>();
	
	static {
		factorial.add(BigInteger.ONE);
	}
	
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
			
			return limitedMatchChance(score, scoreOther, scoreLimit, scoreRate, otherScoreRate);
		}

		private double limitedMatchChance(int a, int b, int limit, double rateA, double rateB) {
			assert a <= limit && b <= limit && !(a == limit && b == limit);
			
			if(a == limit) {
				int steps = 20;
				double[] probabilities = new double[b + 1];
				
				for(int i = 0; i <= steps; i++) {
					double t = 1.0 / steps * i;
					
					//P'(5-0) = scoreRate * P(4-0)
					probabilities[0] += rateA * matchChance(limit - 1, b, rateA * t, rateB * t) / steps;
					
					for(int x = 1; x <= b; x++) { //calculate P'(5-x) = scoreRate * P(4-x) + otherScoreRate * P(5-(x-1))
						probabilities[x] += rateA * matchChance(limit - 1, b, rateA * t, rateB * t) / steps
								+ rateB * probabilities[x - 1] / steps;
					}
				}
				
				return probabilities[b];
			} else if(b == limit) {
				return limitedMatchChance(b, a, limit, rateB, rateA);
			} else {
				return matchChance(a, b, rateA, rateB);
			}
		}
		
		private double matchChance(int a, int b, double rateA, double rateB) {
			return poisson(rateA, a) * poisson(rateB, b);
		}
		
		private double poisson(double lambda, int n) {
			return Math.pow(lambda, n) * Math.exp(-lambda) / factorial(n).doubleValue();
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
		ArrayList<Result> results = new ArrayList<>();
		
		for(int aScore = 0; aScore <= 5; aScore++) {
			for(int bScore = 0; bScore <= 5; bScore++) {
				if(aScore != 5 || bScore != 5)
					results.add(a.new Result(b, aScore, bScore));
			}
		}
		
		results.sort(null);
		
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

package brownshome.ozfortresspredictor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public final class Team {
	private static long[] nCrArray = new long[1000];
	
	private static long nCr(int n, int r) {
		if(r == n || r == 0) return 1;
		if(r > n - r) return nCr(n, n - r);
		
		int entry = n * (n + 1) / 2 + r;
		
		if(nCrArray.length <= entry) {
			int newLength = nCrArray.length;
			do {
				newLength *= 2;
			} while (newLength <= entry);
			nCrArray = Arrays.copyOf(nCrArray, newLength);
		} else if(nCrArray[entry] != 0.0) {
			return nCrArray[entry];
		}
		
		return nCrArray[entry] = nCr(n - 1, r) + nCr(n - 1, r - 1);
	}
	
	private class Result {
		final Team other;
		final int score, scoreOther;
		
		Result(Team other, int score, int scoreOther) {
			this.other = other;
			this.score = score;
			this.scoreOther = scoreOther;
		}
		
		double probability() {
			var p = probabilityOfWin();
			return Math.pow(p, score) * Math.pow(1.0 - p, scoreOther);
		}

		double probabilityOfWin() {
			double totalRank = rank + other.rank;
			return rank / totalRank;
		}
		
		@Override
		public String toString() {
			return String.format("%s(%d) - %s(%d)", Team.this.name, score, other.name, scoreOther);
		}
	}
	
	public final String name;
	private final Collection<Result> matches = new ArrayList<>();

	public double rank;
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

	public double determineRank() {
		double low = 0.0;
		double high = 1.0;
		
		while(high - low > 1e-6) {
			int sections = 4;
			double[] p = new double[sections + 1];
			
			int best = 0;
			double max = 0;
			for(int pi = 0; pi <= sections; pi++) {
				rank = p[pi] = (high * pi + low * (sections - pi)) / sections;
				double prob = probabilityOfResults();
				
				if(prob > max) {
					best = pi;
					max = prob;
				}
			}
			
			rank = p[best];
			
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
}

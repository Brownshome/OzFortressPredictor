package brownshome.ozfortresspredictor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.Map;
import java.util.NavigableSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import brownshome.ozfortresspredictor.Team.Result;

public class CmdInterface {
	public static void main(String[] args) throws IOException {
		System.out.println("Input relationships of the form 'Name(a) - Name(a)'.");
		System.out.println("Write 'calculate' to calculate scores");

		var league = new League();
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		Pattern dataPattern = Pattern.compile("^input (.+)\\((\\d+)\\) - (.+)\\((\\d+)\\)$");
		Pattern requestPattern = Pattern.compile("^predict (.+) - (.+)$");
		Pattern statsPattern = Pattern.compile("^stats (.+)$");

loop: 	while(true) {
			String line = in.readLine();
			if(line.isEmpty()) continue;

			switch(line.trim().toLowerCase()) {
			case "overview":
				System.out.println(teamsToString(league, league.generateScores()));
				continue loop;
			case "exit":
				break loop;
			}

			Matcher data = dataPattern.matcher(line);
			Matcher request = requestPattern.matcher(line);
			Matcher stats = statsPattern.matcher(line);

			if(data.matches()) {
				String teamAName = data.group(1);
				int scoreA = Integer.parseInt(data.group(2));

				String teamBName = data.group(3);
				int scoreB = Integer.parseInt(data.group(4));

				var teamA = league.computeIfAbsent(teamAName, Team::new);
				var teamB = league.computeIfAbsent(teamBName, Team::new);

				Team.addScore(teamA, teamB, scoreA, scoreB);
			} else {
				league.generateScores();
				
				if(request.matches()) {
					var teamA = league.get(request.group(1));
					var teamB = league.get(request.group(2));

					if(teamA == null || teamB == null) {
						System.out.println("No data found for those teams.");
						continue;
					}

					var results = Team.computeLikelyResults(teamA, teamB);
					
					System.out.println(resultsToString(results));
				} else if(stats.matches()) {
					var team = league.get(stats.group(1));
					if(team == null) {
						System.out.println("No data found for that team.");
						continue;
					}

					System.out.println(String.format("%-28s%5.1f%5.1f", team.name, team.attack, team.defence));
				} else {
					System.out.println("Input relationships of the form 'Name(a) - Name(a)'.");
				}
			}
		}
	}

	private static String resultsToString(NavigableSet<Result> results) {
		StringBuilder s = new StringBuilder();

		double sum = results.stream().mapToDouble(Team.Result::probability).sum();
		
		int i = 0;
		s.append("\n************ (Prediction) ************\n");
		for(Team.Result result : results) {
			if(i++ == 5) break;

			s.append(String.format("%-30s%8.1f%%\n", result.toString(), result.probability() * 100.0 / sum));
		}

		return s.toString();
	}

	private static String teamsToString(League league, Map<Team, Collection<Team>> roots) {
		DoubleSummaryStatistics stats = league.values().stream().mapToDouble(team -> Math.log(team.attack * team.defence)).filter(Double::isFinite).summaryStatistics();

		double scale = 1.0 / Math.log(2.0);

		StringBuilder s = new StringBuilder();

		int i = 0;
		for(var root : roots.values()) {
			s.append("\n************ (Log Base 2) ************\n");
			root.stream().sorted((a, b) -> Double.compare(b.attack * b.defence, a.attack * a.defence)).forEach(o -> {
				s.append(String.format("%-30s%8.2f", o.name, (Math.log(o.attack * o.defence) - stats.getMin()) * scale)).append("\n");
			});
		}

		return s.toString();
	}
}

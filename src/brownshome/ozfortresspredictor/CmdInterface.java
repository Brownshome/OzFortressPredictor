package brownshome.ozfortresspredictor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CmdInterface {
	public static void main(String[] args) throws IOException {
		System.out.println("Input relationships of the form 'Name(a) - Name(a)'.");
		System.out.println("Write 'calculate' to calculate scores");
		
		var league = new League();
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		Pattern pattern = Pattern.compile("^(.+)\\((\\d+)\\) - (.+)\\((\\d+)\\)$");
		
		loop:
		while(true) {
			String line = in.readLine();
			if(line.isEmpty()) continue;
			
			switch(line.trim().toLowerCase()) {
			case "calculate":
				System.out.println(teamsToString(league, league.generateScores()));
			case "exit":
				break loop;
			}
			
			Matcher m = pattern.matcher(line);
			if(m.matches()) {
				String teamAName = m.group(1);
				int scoreA = Integer.parseInt(m.group(2));
				
				String teamBName = m.group(3);
				int scoreB = Integer.parseInt(m.group(4));
				
				var teamA = league.computeIfAbsent(teamAName, Team::new);
				var teamB = league.computeIfAbsent(teamBName, Team::new);
				
				Team.addScore(teamA, teamB, scoreA, scoreB);
			} else {
				System.out.println("Input relationships of the form 'Name(a) - Name(a)'.");
			}
		}
	}

	private static String teamsToString(League league, Map<Team, Collection<Team>> roots) {
		DoubleSummaryStatistics stats = league.values().stream().mapToDouble(team -> Math.log(team.rank)).filter(Double::isFinite).summaryStatistics();
		
		double scale = 1.0 / Math.log(2.0);
		
		StringBuilder s = new StringBuilder();
		
		int i = 0;
		for(var root : roots.values()) {
			s.append("\n************ (Log Base 2) ************\n");
			root.stream().sorted((a, b) -> Double.compare(b.rank, a.rank)).forEach(o -> {
				s.append(String.format("%-30s%8.2f", o.name, (Math.log(o.rank) - stats.getMin()) * scale)).append("\n");
			});
		}
		
		return s.toString();
	}
}

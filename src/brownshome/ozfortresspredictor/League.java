package brownshome.ozfortresspredictor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** A league of teams with win-loss relationships between them. */
public class League extends HashMap<String, Team> implements Iterable<Team> {
	public Map<Team, Collection<Team>> generateScores() {
		Map<Team, Collection<Team>> roots = new HashMap<>();
		
		for(Team team : this) {
			team.attack = team.defence = 0.5;
			Team root = team.collapseRoot();
			
			roots.computeIfAbsent(root, t -> new ArrayList<>()).add(team);
		}
		
		double old = 0.0;
		for(int i = 0; i < 1000; i++) {
			double acc = 1.0;
			
			for(Team team : this) {
				acc *= team.determineAttackAndDefence();
			}
			
			if((acc - old) / acc < 1e-6) {
				break;
			}
			
			old = acc;
		}
		
		return roots;
	}

	@Override
	public Iterator<Team> iterator() {
		return values().iterator();
	}
}

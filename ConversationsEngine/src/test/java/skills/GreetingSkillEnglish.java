package skills;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.json.JSONObject;

import interfaces.ISkill;
import interfaces.ISkillAnswer;
import interfaces_implementation.SkillAnswer;

/**
 * A {@link ISkill skill} that generates a greeting message
 * 
 * @author Marcel Engelmann
 *
 */
public class GreetingSkillEnglish implements ISkill {

	@Override
	public ISkillAnswer execute(String intent, JSONObject contextObject, String currentState) {
		if (intent.equals("greeting")) {
			int currentHour = Calendar.HOUR_OF_DAY;
			List<String> dayTime = new ArrayList<>();
			if (currentHour <= 10) {
				dayTime.add("Good morning!");
			} else if (currentHour <= 16) {
				dayTime.add("Good afternoon!");
			} else if (currentHour <= 19) {
				dayTime.add("Good evening!");
			} else {
				dayTime.add("Hello!");
			}

			return new SkillAnswer("SUCCESS", dayTime, false);
		}

		return null;
	}

	@Override
	public boolean canExecute(String intent, String currentState) {
		return intent.equalsIgnoreCase("greeting");
	}

	@Override
	public void reset() {
		// nothing to do
	}
}

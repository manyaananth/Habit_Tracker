package com.example.data

import java.util.Calendar

data class Concept(
    val id: String,
    val title: String,
    val category: String,
    val emoji: String,
    val summary: String,
    val longDescription: String,
    val scientificFact: String,
    val xpReward: Int = 15
)

object ConceptsRepository {
    val allPredefinedConcepts = listOf(
        Concept(
            id = "cue_loop",
            title = "The Habit Loop Mechanics",
            category = "Habit Loop Science",
            emoji = "🔁",
            summary = "Every habit follows an invisible 3-step neural sequence: Cue, Routine, and Reward.",
            longDescription = "To build a habit, design a clear cue and clear reward. The cue is a trigger (like a time, place, or immediate past event) that prompts your brain to go into automatic pilot. The routine is the direct behavior you execute. The reward is a satisfaction indicator that teaches your brain to lock in this pattern for future cues.",
            scientificFact = "Coined by Nobel-eligible neuroscientists, this cue-routine-reward loop is backed by basal ganglia research and helps bypass manual cognitive fatigue."
        ),
        Concept(
            id = "habit_stacking",
            title = "Atomic Habit Stacking",
            category = "Implementation Science",
            emoji = "🥞",
            summary = "Anchor a new custom habit directly behind an old, solid existing automatic behavior.",
            longDescription = "Instead of linking your habit to broad schedules like 'afternoons', anchor it directly behind a specific action you do automatically every day (e.g., 'Immediately after I close my laptop door, I will stretch for 5 minutes'). This uses the momentum of existing neural pathways to install the new behaviors.",
            scientificFact = "Developed by behaviorist BJ Fogg and popularized by James Clear, this method reduces initiation friction by leveraging active dopamine cues."
        ),
        Concept(
            id = "implementation_intentions",
            title = "Implementation Intentions",
            category = "Tactical Execution",
            emoji = "🎯",
            summary = "Specify exactly 'When', 'Where', and 'How' you will execute the action to double completion rates.",
            longDescription = "Vague goals like 'I will work out today' frequently fail. By stating 'If [situation arises], then I will [perform reaction]', your brain pre-installs the decision-making logic well in advance, avoiding choice paralysis in moments of exhaust.",
            scientificFact = "Over 100 psychological studies show that utilizing \"if-then\" planning structures increases goal completion rates from 35% to nearly 74%."
        ),
        Concept(
            id = "two_minute_rule",
            title = "The 2-Minute Gateway",
            category = "Friction Reduction",
            emoji = "⏳",
            summary = "When starting any new habit, scale it down to an action that takes less than 2 minutes.",
            longDescription = "Instead of 'Read one entire chapter', practice 'Read 1 single page'. Instead of 'Jog 5 miles', practice 'Put on running shoes'. By shrinking the barrier of initiation, you build the ritual first. Once the cue-habit stack becomes automatic, incremental scaling becomes remarkably simple.",
            scientificFact = "A behavior must be established before it can be improved. Focus on the gateway ritual rather than the overall task duration."
        ),
        Concept(
            id = "friction_modeling",
            title = "Friction Optimization",
            category = "Environment Design",
            emoji = "⛰️",
            summary = "Re-engineer your physical space: decrease friction for good habits, and maximize friction for bad ones.",
            longDescription = "Our brains are designed to conserve physical energy. If you want to drink more water, place insulated bottles directly on your desk. If you want to check your phone less, lock it in a completely separate drawer. Create environments where optimal choices require the least potential cognitive resistance.",
            scientificFact = "Psychologist Kurt Lewin's Force Field Analysis shows that reducing negative forces (resistance/friction) is vastly more effective than increasing driving forces."
        ),
        Concept(
            id = "identity_habits",
            title = "Identity-Based Systems",
            category = "Core Self-Belief",
            emoji = "👤",
            summary = "Focus on the type of person you want to become, rather than the target goals you want to hit.",
            longDescription = "Goal-based outcomes ('I want to read 10 books') are fragile. Identity-based processes ('I am a Reader') are highly resilient. Every single positive small action you take serves as a single democratic vote for your new character. Shift your system focus from outcomes to identity reinforcement.",
            scientificFact = "Self-discrepancy theory shows that actions aligned with our internalized self-schema (identity) require zero willpower to maintain."
        ),
        Concept(
            id = "progress_principle",
            title = "The Progress Principle",
            category = "Intrinsic Reward",
            emoji = "📈",
            summary = "Sustaining success requires feeling that you are continuously moving forward in meaningful tasks.",
            longDescription = "Even minor progress increases intrinsic motivation, happiness, and creative execution throughout your day. Keep track of small daily wins—like checking off a 10-minute walk—to flood your neurological system with feedback cues of consistent execution.",
            scientificFact = "Harvard Business School studies demonstrate that tracking small wins is the single most powerful driver of employee motivation."
        ),
        Concept(
            id = "willpower_energy",
            title = "Willpower Renewal Theory",
            category = "Willpower Science",
            emoji = "🔋",
            summary = "Willpower is not a rigid resource that depletes easily; it's heavily guided by belief and motivation.",
            longDescription = "While standard theory suggested willpower is a limited fuel like glucose (ego depletion), modern research shows that how you view distraction is key. When you frame habit routines as highly valuable, enjoyable rewards rather than grueling obligations, the felt mental friction drops dramatically.",
            scientificFact = "Recent replication analyses show that ego depletion disappears when subjects are told tasks are enjoyable and self-aligning."
        ),
        Concept(
            id = "goldilocks_rule",
            title = "The Goldilocks Flow Zone",
            category = "Attention & Focus",
            emoji = "⚖️",
            summary = "To stay motivated, work on tasks of just manageable difficulty—neither too आसान nor too hard.",
            longDescription = "Tasks that are too easy lead to boredom. Tasks that are too hard cause severe frustration and escape. Seek projects that sit directly on the knife-edge of your present abilities (roughly 4% above comfort). This generates an optimal focus state known as Flow.",
            scientificFact = "Flow state research by Mihaly Csikszentmihalyi reveals that staying in the challenge-skill balance triggers elevated releases of norepinephrine."
        ),
        Concept(
            id = "commitment_devices",
            title = "Ulysses Commitment Devices",
            category = "Pre-commitment",
            emoji = "⚓",
            summary = "Make a firm choice in the present to completely bind your behavior in the future.",
            longDescription = "Named after ancient hero Ulysses, who tied himself to his ship's mast to resist the Sirens' songs. If you seek to save money, configure automated bank transfers to investment apps immediately on payday. Lock in healthy actions in moments of logical strength so temptation has zero space.",
            scientificFact = "Behavioral economics confirms that pre-commitment devices neutralize the human tendency of hyperbolic discounting (preferring instant gratification)."
        )
    )

    fun getDailyConcepts(dayOfYear: Int): List<Concept> {
        val count = allPredefinedConcepts.size
        val idx1 = Math.abs(dayOfYear) % count
        val idx2 = java.lang.Math.abs(dayOfYear + 1) % count
        // Return two unique concepts
        return if (idx1 == idx2 && count > 1) {
            listOf(allPredefinedConcepts[idx1], allPredefinedConcepts[(idx1 + 1) % count])
        } else {
            listOf(allPredefinedConcepts[idx1], allPredefinedConcepts[idx2])
        }
    }

    fun getDailyConceptsForToday(): List<Concept> {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_YEAR) + (calendar.get(Calendar.YEAR) * 365)
        return getDailyConcepts(day)
    }
}

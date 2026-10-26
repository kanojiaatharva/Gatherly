package com.gatherly.ai;

import com.gatherly.budget.BudgetItem;
import com.gatherly.events.Event;
import com.gatherly.tasks.Task;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class DeterministicRuleBasedPlanningProvider implements AiPlanningProvider {

    @Override
    public String getProviderMode() {
        return "DEVELOPMENT_MOCK";
    }

    @Override
    public GeneratedPlanResponse generatePlan(Event event, String promptNotes) {
        String type = event.getEventType() != null ? event.getEventType().toLowerCase() : "birthday";
        BigDecimal targetBudget = event.getTotalBudget() != null && event.getTotalBudget().compareTo(BigDecimal.ZERO) > 0
                ? event.getTotalBudget()
                : (event.getGuestCount() != null ? BigDecimal.valueOf(event.getGuestCount() * 800L) : new BigDecimal("50000.00"));

        // Realistic baseline floor per guest to ensure realistic estimates are maintained
        BigDecimal minimumRealisticBudget;
        if (type.contains("wedding")) {
            long guests = event.getGuestCount() != null ? Math.max(1, event.getGuestCount()) : 50;
            minimumRealisticBudget = BigDecimal.valueOf(guests * 1000L).setScale(2, RoundingMode.HALF_UP);
        } else if (type.contains("community")) {
            long guests = event.getGuestCount() != null ? Math.max(1, event.getGuestCount()) : 50;
            minimumRealisticBudget = BigDecimal.valueOf(guests * 400L).setScale(2, RoundingMode.HALF_UP);
        } else {
            long guests = event.getGuestCount() != null ? Math.max(1, event.getGuestCount()) : 30;
            minimumRealisticBudget = BigDecimal.valueOf(guests * 500L).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal planningBase = targetBudget.compareTo(minimumRealisticBudget) < 0
                ? minimumRealisticBudget
                : targetBudget;

        LocalDate eventDate = event.getEventDate() != null ? event.getEventDate() : LocalDate.now().plusMonths(2);
        LocalDate today = LocalDate.now();

        // 1. Missing Essential Info & Follow-up Questions Detection
        List<String> followUpQuestions = new ArrayList<>();
        List<String> assumptions = new ArrayList<>();

        if (event.getEventDate() == null) {
            followUpQuestions.add("What is your target celebration date or preferred month?");
            assumptions.add("Assumed event date is approximately 60 days from today (" + eventDate + ").");
        } else {
            assumptions.add("Event date anchored to " + eventDate + " (" + java.time.temporal.ChronoUnit.DAYS.between(today, eventDate) + " days out).");
        }

        if (event.getLocation() == null || event.getLocation().isBlank()) {
            followUpQuestions.add("Which city or neighborhood are you considering for this celebration?");
            assumptions.add("Assumed metropolitan venue location with standard vendor accessibility.");
        }

        if (event.getGuestCount() == null || event.getGuestCount() <= 0) {
            followUpQuestions.add("Approximately how many guests are you expecting?");
            assumptions.add("Assumed baseline guest count of 50 attendees.");
        } else {
            assumptions.add("Calculated per-guest allocations based on " + event.getGuestCount() + " expected attendees.");
        }

        if (event.getTotalBudget() == null || event.getTotalBudget().compareTo(BigDecimal.ZERO) <= 0) {
            followUpQuestions.add("What is your total target budget and preferred currency?");
            assumptions.add("Assumed default allocation budget of " + targetBudget + " " + event.getCurrency() + ".");
        }

        if (event.getStylePreferences() == null || event.getStylePreferences().isBlank()) {
            followUpQuestions.add("Do you have a preferred theme, color palette, or atmosphere in mind?");
            assumptions.add("Applied classic contemporary styling tailored for " + event.getEventType() + ".");
        }

        assumptions.add("All generated budget allocations are marked as estimates and require verified quotes prior to contract signing.");

        // 2. Build Domain-Specific Content
        List<GeneratedBudgetItem> budgetItems = new ArrayList<>();
        List<GeneratedTask> tasks = new ArrayList<>();
        List<GeneratedIdea> ideas = new ArrayList<>();
        List<GeneratedVendorCategory> vendorCategories = new ArrayList<>();
        List<String> themeSuggestions = new ArrayList<>();
        List<GeneratedTimelinePhase> timeline = new ArrayList<>();
        String summary;

        if (type.contains("wedding")) {
            summary = "An elegant, romantic wedding celebration designed for unforgettable guest hospitality, cohesive styling, and balanced budget distribution.";
            themeSuggestions.addAll(List.of("Modern Botanical Elegance", "Classic Warm Neutral & Gold", "Rustic Garden Romance"));

            budgetItems.add(new GeneratedBudgetItem("Venue & Rentals", "Reception & ceremony hall hire (Estimate)", calc(planningBase, 0.34)));
            budgetItems.add(new GeneratedBudgetItem("Catering & Drinks", "Multi-course meal & signature welcome bar (Estimate)", calc(planningBase, 0.30)));
            budgetItems.add(new GeneratedBudgetItem("Photography & Video", "Full day coverage with digital gallery (Estimate)", calc(planningBase, 0.12)));
            budgetItems.add(new GeneratedBudgetItem("Florals & Decor", "Ceremony arch, table centerpieces, and ambiance lighting (Estimate)", calc(planningBase, 0.09)));
            budgetItems.add(new GeneratedBudgetItem("Music & Entertainment", "Live ceremony musician & reception DJ (Estimate)", calc(planningBase, 0.07)));
            budgetItems.add(new GeneratedBudgetItem("Attire & Styling", "Bridal wear, groom suit, and styling essentials (Estimate)", calc(planningBase, 0.05)));
            budgetItems.add(new GeneratedBudgetItem("Contingency & Gifts", "Favors, emergency kit, and miscellaneous buffer (Estimate)", calc(planningBase, 0.03)));

            tasks.add(new GeneratedTask("Tour & book venue", "Confirm date availability, deposit terms, and curfew regulations", targetDate(eventDate, today, 75), "URGENT", "Couples"));
            tasks.add(new GeneratedTask("Hire wedding photographer", "Review portfolios, lighting styles, and contract deliverables", targetDate(eventDate, today, 60), "HIGH", "Couples"));
            tasks.add(new GeneratedTask("Schedule catering tasting", "Sample menu options and accommodate dietary preferences", targetDate(eventDate, today, 45), "HIGH", "Catering Lead"));
            tasks.add(new GeneratedTask("Send formal invitations", "Distribute digital/paper invites with RSVP deadline 3 weeks prior", targetDate(eventDate, today, 35), "MEDIUM", "Host"));
            tasks.add(new GeneratedTask("Finalize floral & table arrangements", "Align color palette with venue coordinator", targetDate(eventDate, today, 20), "MEDIUM", "Decorator"));
            tasks.add(new GeneratedTask("Confirm final headcount with caterer", "Submit final guest list including dietary breakdown", targetDate(eventDate, today, 10), "HIGH", "Catering Lead"));
            tasks.add(new GeneratedTask("Rehearsal & timeline walkthrough", "Run through procession, music cues, and vendor contacts", targetDate(eventDate, today, 2), "HIGH", "All"));

            ideas.add(new GeneratedIdea("Guest Experience", "Polaroid Guestbook Corner", "Provide vintage-style instant cameras with metallic pens so guests leave a photo and handwritten message."));
            ideas.add(new GeneratedIdea("Catering & Drinks", "Signature Couples Cocktails", "Create two bespoke beverages named after memorable milestones in your story."));
            ideas.add(new GeneratedIdea("Atmosphere", "Acoustic Sunset Interlude", "Host a 30-minute acoustic set while photos are taken during golden hour before dinner."));

            vendorCategories.add(new GeneratedVendorCategory("Reception Venue", "Spaces capable of accommodating seated guests with sound permits", List.of("Ask about noise curfews", "Check outside vendor policies", "Inquire about bridal suite access")));
            vendorCategories.add(new GeneratedVendorCategory("Catering & Bar", "Full-service caterers with buffet and plated options", List.of("Ask for complimentary tastings", "Confirm server-to-guest ratio", "Check corkage fee policies")));
            vendorCategories.add(new GeneratedVendorCategory("Photographer & Videographer", "Storytelling photographers specialized in natural lighting", List.of("Request a full wedding gallery (not just highlights)", "Check turnaround time for final edits")));

            timeline.add(new GeneratedTimelinePhase("Phase 1: Foundation & Major Vendors", "8-12 weeks before", List.of("Secure reception & ceremony venue", "Contract lead photographer", "Select core color palette")));
            timeline.add(new GeneratedTimelinePhase("Phase 2: Coordination & Invitations", "4-6 weeks before", List.of("Distribute formal invitations with RSVP deadline", "Complete catering and cake tasting", "Reserve musical entertainment")));
            timeline.add(new GeneratedTimelinePhase("Phase 3: Final Countdown & Logistics", "1-2 weeks before", List.of("Submit final headcount to caterer", "Confirm day-of vendor schedule", "Host wedding rehearsal")));
            timeline.add(new GeneratedTimelinePhase("Phase 4: Wedding Day", "Day of event", List.of("Morning hair & styling", "Ceremony procession", "Dinner, speeches & dancing celebration")));

        } else if (type.contains("community")) {
            summary = "An inclusive, engaging community gathering centered on neighborhood connection, accessibility, and smooth crowd logistics.";
            themeSuggestions.addAll(List.of("Community Harvest & Unity", "Local Arts & Culture Showcase", "Green Living & Neighborhood Connect"));

            budgetItems.add(new GeneratedBudgetItem("Venue & Permits", "Community hall, park reservation, and municipal permits (Estimate)", calc(planningBase, 0.32)));
            budgetItems.add(new GeneratedBudgetItem("Food & Refreshments", "Local community buffet, coffee/tea station, and finger foods (Estimate)", calc(planningBase, 0.32)));
            budgetItems.add(new GeneratedBudgetItem("Sound & Stage AV", "PA system, wireless microphones, and projector (Estimate)", calc(planningBase, 0.16)));
            budgetItems.add(new GeneratedBudgetItem("Signage & Promotion", "Welcome banners, directions, and informational leaflets (Estimate)", calc(planningBase, 0.10)));
            budgetItems.add(new GeneratedBudgetItem("Sanitation & Safety", "First-aid station, recycling bins, and cleaning crew (Estimate)", calc(planningBase, 0.06)));
            budgetItems.add(new GeneratedBudgetItem("Buffer & Supplies", "Nametags, volunteer badges, and spare cables (Estimate)", calc(planningBase, 0.04)));

            tasks.add(new GeneratedTask("Secure municipal permit & venue", "File official application with local council/park authority", targetDate(eventDate, today, 45), "URGENT", "Lead Organizer"));
            tasks.add(new GeneratedTask("Confirm sound/PA equipment", "Test wireless microphones, speaker output, and generator backup", targetDate(eventDate, today, 25), "HIGH", "Tech Coordinator"));
            tasks.add(new GeneratedTask("Publish community signup & flyers", "Distribute notice on neighborhood forums and social channels", targetDate(eventDate, today, 20), "MEDIUM", "Communications"));
            tasks.add(new GeneratedTask("Assign volunteer stations", "Brief volunteers on registration, wayfinding, and accessibility needs", targetDate(eventDate, today, 7), "HIGH", "Volunteer Lead"));
            tasks.add(new GeneratedTask("Safety walk & equipment check", "Inspect wheelchair ramps, emergency exits, and power cables", targetDate(eventDate, today, 1), "HIGH", "Safety Lead"));

            ideas.add(new GeneratedIdea("Community Engagement", "Neighborhood Memory Wall", "A giant collaborative chalkboard or corkboard where attendees pin photos, wishes, and local stories."));
            ideas.add(new GeneratedIdea("Accessibility", "Sensory-Friendly Quiet Corner", "A shaded, comfortable zone with low noise levels for elders, families with toddlers, or sensory-sensitive guests."));
            ideas.add(new GeneratedIdea("Zero Waste", "Bring Your Own Cup Initiative", "Offer a friendly raffle ticket to any attendee who brings a reusable bottle or mug."));

            vendorCategories.add(new GeneratedVendorCategory("Community Venue", "Open public park or civic hall with accessibility ramps", List.of("Check maximum capacity limits", "Confirm sound amplification rules")));
            vendorCategories.add(new GeneratedVendorCategory("Sound / AV Rental", "Microphones and public address systems", List.of("Request on-site technician support", "Inquire about weather protection for outdoor gear")));

            timeline.add(new GeneratedTimelinePhase("Phase 1: Permits & Structure", "6-8 weeks before", List.of("Submit municipal park permit", "Form volunteer committee", "Confirm public liability coverage")));
            timeline.add(new GeneratedTimelinePhase("Phase 2: Outreach & Equipment", "3-4 weeks before", List.of("Launch registration and flyer distribution", "Reserve sound/PA equipment", "Confirm accessible restroom facilities")));
            timeline.add(new GeneratedTimelinePhase("Phase 3: Final Briefings", "1 week before", List.of("Conduct volunteer walkthrough", "Test electrical outlets and extension lines", "Finalize wayfinding signage")));
            timeline.add(new GeneratedTimelinePhase("Phase 4: Event Execution", "Day of event", List.of("Site opening and safety inspection", "Community activities & talks", "Zero-waste cleanup & pack down")));

        } else {
            // Birthday / Milestone celebration
            summary = "A vibrant and joyful birthday celebration with lively entertainment, delicious catering, and stress-free coordination.";
            themeSuggestions.addAll(List.of("Warm Botanical Chic", "Retro Disco Fiesta", "Minimalist Sunset Lounge"));

            budgetItems.add(new GeneratedBudgetItem("Venue & Setup", "Private dining space or terrace venue rental (Estimate)", calc(planningBase, 0.32)));
            budgetItems.add(new GeneratedBudgetItem("Food & Catering", "Curated buffet or tapas with vegetarian highlights (Estimate)", calc(planningBase, 0.34)));
            budgetItems.add(new GeneratedBudgetItem("Celebration Cake & Sweets", "Custom designer cake and dessert table (Estimate)", calc(planningBase, 0.09)));
            budgetItems.add(new GeneratedBudgetItem("Entertainment & Music", "Curated sound system or DJ performance (Estimate)", calc(planningBase, 0.12)));
            budgetItems.add(new GeneratedBudgetItem("Decor & Photobooth", "Custom backdrop, fairy lights, and themed table styling (Estimate)", calc(planningBase, 0.08)));
            budgetItems.add(new GeneratedBudgetItem("Party Favors & Games", "Thank-you tokens, interactive game props (Estimate)", calc(planningBase, 0.05)));

            tasks.add(new GeneratedTask("Reserve celebration venue", "Check booking deposit and outside catering rules", targetDate(eventDate, today, 30), "URGENT", "Host"));
            tasks.add(new GeneratedTask("Order custom birthday cake", "Select flavors, tier size, and dietary considerations", targetDate(eventDate, today, 18), "HIGH", "Host"));
            tasks.add(new GeneratedTask("Send invitations & track RSVPs", "Send digital invitations with calendar link", targetDate(eventDate, today, 15), "MEDIUM", "Host"));
            tasks.add(new GeneratedTask("Curate birthday playlist", "Compile upbeat tracks and party anthems across genres", targetDate(eventDate, today, 8), "LOW", "Friend / Host"));
            tasks.add(new GeneratedTask("Confirm headcount & seating", "Inform caterer of confirmed attendees and allergies", targetDate(eventDate, today, 5), "HIGH", "Host"));
            tasks.add(new GeneratedTask("Pick up cake & decor props", "Collect cake safely and ensure cooler storage", targetDate(eventDate, today, 1), "HIGH", "Host"));

            ideas.add(new GeneratedIdea("Entertainment", "Personal Trivia Challenge", "A 10-question mobile trivia game about the birthday star with small fun prizes for the winners."));
            ideas.add(new GeneratedIdea("Memory Keepsake", "Time Capsule Message Box", "Guests write predictions and warm wishes to be opened on a future milestone."));
            ideas.add(new GeneratedIdea("Dessert", "Interactive DIY Dessert Station", "A build-your-own sundae, waffle, or churro cart with assorted toppings."));

            vendorCategories.add(new GeneratedVendorCategory("Private Event Venue", "Terraces, bistros, or garden spaces", List.of("Inquire about minimum spend", "Check BYO cake policies")));
            vendorCategories.add(new GeneratedVendorCategory("Custom Bakery", "Specialty celebration cakes", List.of("Ask about allergen-free options", "Confirm refrigerated delivery")));
            vendorCategories.add(new GeneratedVendorCategory("Party Decor & Balloons", "Backdrop installations and table styling", List.of("Inquire about setup and tear-down services")));

            timeline.add(new GeneratedTimelinePhase("Phase 1: Concept & Venue", "4-6 weeks before", List.of("Book private party venue or terrace", "Pick celebration aesthetic and color scheme", "Send save-the-dates")));
            timeline.add(new GeneratedTimelinePhase("Phase 2: Catering & Details", "2-3 weeks before", List.of("Order specialty birthday cake", "Order customized decorations and favors", "Confirm playlist and music setup")));
            timeline.add(new GeneratedTimelinePhase("Phase 3: Final Touches", "3-5 days before", List.of("Lock final attendee headcount", "Review food allergy accommodations with kitchen", "Prep party games and trivia props")));
            timeline.add(new GeneratedTimelinePhase("Phase 4: Celebration Time", "Party Day", List.of("Venue styling and balloon setup", "Welcome guests with signature drinks", "Cake cutting, music & festivities")));
        }

        // 3. Compute totals with BigDecimal
        BigDecimal calculatedTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (GeneratedBudgetItem item : budgetItems) {
            calculatedTotal = calculatedTotal.add(item.estimatedAmount());
        }

        // 4. Over-budget checking and trade-offs
        boolean over = event.getTotalBudget() != null
                && event.getTotalBudget().compareTo(BigDecimal.ZERO) > 0
                && calculatedTotal.compareTo(event.getTotalBudget()) > 0;

        String warning = null;
        List<String> tradeOffSuggestions = new ArrayList<>();

        if (over) {
            BigDecimal diff = calculatedTotal.subtract(event.getTotalBudget()).setScale(2, RoundingMode.HALF_UP);
            warning = "The proposed plan estimated allocations (" + calculatedTotal + " " + event.getCurrency()
                    + ") exceed your target budget (" + event.getTotalBudget() + " " + event.getCurrency()
                    + ") by " + diff + " " + event.getCurrency() + ". Your target budget was preserved.";
            tradeOffSuggestions.add("Opt for buffet or family-style dinner instead of individual multi-course plated service (saves ~15-20% on catering).");
            tradeOffSuggestions.add("Select a naturally scenic venue that requires minimal supplemental floral/lighting rentals.");
            tradeOffSuggestions.add("Consolidate floral budget into primary ceremony arch and main table rather than all perimeter stations.");
            tradeOffSuggestions.add("Hire a curated DJ or acoustic duo instead of a full multi-piece live band.");
        }

        return new GeneratedPlanResponse(
                summary,
                assumptions,
                themeSuggestions,
                budgetItems,
                tasks,
                timeline,
                ideas,
                vendorCategories,
                warning,
                tradeOffSuggestions,
                calculatedTotal,
                over,
                followUpQuestions,
                getProviderMode(),
                "DEVELOPMENT MOCK: AI credentials not configured in environment. Generated using deterministic rule-based planning engine."
        );
    }

    @Override
    public PlanRevisionProposal proposeRevision(
            Event event,
            List<BudgetItem> currentBudget,
            List<Task> currentTasks,
            String userInstruction) {
        String lower = userInstruction.toLowerCase();
        List<ProposedBudgetItemDiff> budgetDiffs = new ArrayList<>();
        List<ProposedTaskDiff> taskDiffs = new ArrayList<>();
        List<String> newVendorCategories = new ArrayList<>();
        List<GeneratedIdea> newIdeas = new ArrayList<>();
        StringBuilder summary = new StringBuilder("Proposed plan revisions based on: \"" + userInstruction.trim() + "\". ");

        BigDecimal currentTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (BudgetItem b : currentBudget) {
            if (b.getEstimatedAmount() != null) {
                currentTotal = currentTotal.add(b.getEstimatedAmount());
            }
        }
        BigDecimal proposedTotal = currentTotal;

        int preservedManualCount = 0;
        if (lower.contains("cut") || lower.contains("reduce") || lower.contains("trim") || lower.contains("lower") || lower.contains("budget")) {
            for (BudgetItem b : currentBudget) {
                boolean isManualOrConfirmed = Boolean.FALSE.equals(b.getIsEstimate()) || b.getActualAmount() != null;
                if (isManualOrConfirmed) {
                    preservedManualCount++;
                    continue; // Preserve manual and confirmed budget entries untouched
                }
                if (b.getCategory().toLowerCase().contains("decor") || b.getCategory().toLowerCase().contains("music") || b.getCategory().toLowerCase().contains("contingency") || b.getCategory().toLowerCase().contains("favors")) {
                    BigDecimal oldAmt = b.getEstimatedAmount() != null ? b.getEstimatedAmount() : BigDecimal.ZERO;
                    BigDecimal newAmt = oldAmt.multiply(new BigDecimal("0.75")).setScale(2, RoundingMode.HALF_UP);
                    budgetDiffs.add(new ProposedBudgetItemDiff("UPDATE", b.getCategory(), b.getDescription(), oldAmt, newAmt, "Trimmed discretionary allocation by 25%"));
                    proposedTotal = proposedTotal.subtract(oldAmt).add(newAmt);
                }
            }
            if (preservedManualCount > 0) {
                summary.append("Preserved ").append(preservedManualCount).append(" manually confirmed budget item(s). ");
            }
            summary.append("Trimmed discretionary categories by ~25% to protect overall budget. ");
        }

        if (lower.contains("music") || lower.contains("band") || lower.contains("dj") || lower.contains("acoustic")) {
            boolean alreadyExists = currentTasks.stream().anyMatch(t -> t.getTitle().toLowerCase().contains("acoustic"));
            if (!alreadyExists) {
                LocalDate due = event.getEventDate() != null ? event.getEventDate().minusDays(25) : LocalDate.now().plusDays(20);
                taskDiffs.add(new ProposedTaskDiff("ADD", "Audition live acoustic musicians", "Review demo reels and verify repertoire meets celebration tone", due, "HIGH", "Live music request"));
                newVendorCategories.add("Live Acoustic Musicians");
                newIdeas.add(new GeneratedIdea("Music", "Sunset Acoustic Performance", "Feature a 45-minute live acoustic performance during welcome drinks."));
                summary.append("Added live acoustic music coordination tasks and ideas. ");
            }
        }

        if (lower.contains("vegan") || lower.contains("vegetarian") || lower.contains("diet") || lower.contains("organic")) {
            boolean alreadyExists = currentTasks.stream().anyMatch(t -> t.getTitle().toLowerCase().contains("plant-forward") || t.getTitle().toLowerCase().contains("vegan"));
            if (!alreadyExists) {
                LocalDate due = event.getEventDate() != null ? event.getEventDate().minusDays(18) : LocalDate.now().plusDays(15);
                taskDiffs.add(new ProposedTaskDiff("ADD", "Curate plant-forward seasonal menu", "Coordinate with caterer for innovative vegetarian/vegan highlights", due, "HIGH", "Dietary request"));
                newIdeas.add(new GeneratedIdea("Catering", "Farm-to-Table Grazing Station", "Display colorful organic vegetables, artisanal cheeses, and homemade dips."));
                summary.append("Enhanced catering tasks with organic and dietary-friendly requirements. ");
            }
        }

        if (budgetDiffs.isEmpty() && taskDiffs.isEmpty()) {
            LocalDate due = event.getEventDate() != null ? event.getEventDate().minusDays(14) : LocalDate.now().plusDays(14);
            taskDiffs.add(new ProposedTaskDiff("ADD", "Address instruction: " + userInstruction, "Review specifics with planning team", due, "MEDIUM", "User instruction"));
            summary.append("Added task to incorporate requested preferences. ");
        }

        boolean isOver = event.getTotalBudget() != null
                && event.getTotalBudget().compareTo(BigDecimal.ZERO) > 0
                && proposedTotal.compareTo(event.getTotalBudget()) > 0;

        return new PlanRevisionProposal(
                event.getId(),
                userInstruction,
                summary.toString().trim(),
                budgetDiffs,
                taskDiffs,
                newVendorCategories,
                newIdeas,
                proposedTotal,
                isOver
        );
    }

    private BigDecimal calc(BigDecimal total, double ratio) {
        return total.multiply(BigDecimal.valueOf(ratio)).setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDate targetDate(LocalDate eventDate, LocalDate today, int daysBefore) {
        LocalDate calculated = eventDate.minusDays(daysBefore);
        if (calculated.isBefore(today)) {
            return today.plusDays(3);
        }
        return calculated;
    }
}

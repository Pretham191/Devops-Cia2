package com.beforeyougo.config;

import com.beforeyougo.domain.Category;
import com.beforeyougo.domain.Contribution;
import com.beforeyougo.domain.ContributionStatus;
import com.beforeyougo.domain.ContributionType;
import com.beforeyougo.domain.Institution;
import com.beforeyougo.domain.ServiceGuide;
import com.beforeyougo.repository.ContributionRepository;
import com.beforeyougo.repository.InstitutionRepository;
import com.beforeyougo.repository.ServiceGuideRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Loads sample Chennai data when the database is empty (always on H2; first start on Postgres).
 * Every seed guide is UNVERIFIED and the site shows a "sample data" notice: these are illustrations,
 * not official requirements. lastVerifiedAt dates are relative to today so all three badges appear.
 */
@Component
@ConditionalOnBooleanProperty(name = "app.seed.enabled", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final InstitutionRepository institutions;
    private final ServiceGuideRepository guides;
    private final ContributionRepository contributions;
    private final Clock clock;

    public DataSeeder(InstitutionRepository institutions, ServiceGuideRepository guides,
                      ContributionRepository contributions, Clock clock) {
        this.institutions = institutions;
        this.guides = guides;
        this.contributions = contributions;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (institutions.count() > 0) {
            log.info("Database already has data; skipping seed");
            return;
        }
        LocalDate today = LocalDate.now(clock);

        Institution psk = institution("Passport Seva Kendra, Aminjikarai", Category.GOVERNMENT, "Chennai",
                "Aminjikarai, Chennai (sample address)", null,
                "Passport application and renewal centre run under the passport office.");
        Institution rto = institution("Regional Transport Office, K.K. Nagar", Category.GOVERNMENT, "Chennai",
                "K.K. Nagar, Chennai (sample address)", null,
                "Driving licences, vehicle registration and related services.");
        Institution bank = institution("Nationalised Bank, T. Nagar Branch", Category.BANKING, "Chennai",
                "T. Nagar, Chennai (sample address)", "Branch phone: check the bank's website",
                "Retail banking branch: accounts, deposits and customer service.");
        Institution uni = institution("University Registrar's Office", Category.EDUCATION, "Chennai",
                "Main campus administrative block, Chennai (sample address)", null,
                "Academic records: transcripts, degree certificates and verification letters.");
        Institution hospital = institution("Government General Hospital — Outpatient Department", Category.HEALTHCARE, "Chennai",
                "Park Town, Chennai (sample address)", null,
                "Outpatient registration and consultations at a public hospital.");

        ServiceGuide passport = guide(psk, "Fresh passport application (normal)",
                "Apply for a first-time passport and attend the appointment at the Passport Seva Kendra.",
                List.of("Printed appointment confirmation", "Proof of present address (original + self-attested copy)",
                        "Proof of date of birth (original + self-attested copy)", "Photo ID card"),
                List.of("Register on the online passport portal", "Fill and submit the application form online",
                        "Pay the fee online and book an appointment slot"),
                List.of("Arrive about 15 minutes before your slot", "Token issued at the entrance after a document check",
                        "Document verification, biometrics and photo at counters", "Collect the acknowledgement slip before leaving"),
                List.of("Bringing only photocopies without the originals", "Name or date of birth not matching across documents",
                        "Arriving late and missing the appointment slot"),
                "Paid online while booking; check the official portal for the current amount",
                "Online appointment required", null,
                today.minusDays(25));
        guide(psk, "Passport re-issue (renewal)",
                "Renew a passport that has expired or is about to expire.",
                List.of("Old passport (original)", "Printed appointment confirmation", "Proof of present address if it has changed"),
                List.of("Submit the re-issue application online", "Book an appointment"),
                List.of("Document check at entry", "Verification, biometrics and photo"),
                List.of("Forgetting the old passport", "Not updating the address when it has changed"),
                null, "Online appointment required", null,
                today.minusDays(140));
        ServiceGuide licence = guide(rto, "Driving licence renewal",
                "Renew a driving licence that has expired or is close to expiry.",
                List.of("Existing driving licence", "Proof of address", "Passport-size photographs",
                        "Medical certificate (if required for your age group)"),
                List.of("Apply on the transport services portal", "Pay the fee online"),
                List.of("Submit documents at the counter", "Biometric capture and photo, if requested"),
                List.of("Waiting too long after expiry — extra rules can apply", "Missing the medical certificate when it is needed"),
                null, null, null,
                today.minusDays(240));
        guide(rto, "Learner's licence test",
                "Take the learner's licence test before applying for a permanent licence.",
                List.of("Application printout", "Proof of age", "Proof of address", "Photographs"),
                List.of("Apply online and book a test slot", "Study the road-rules material"),
                List.of("Document verification", "Computer-based road-rules test"),
                List.of("Not preparing for the road-signs questions"),
                null, "Test slot booked online", null,
                null);
        ServiceGuide account = guide(bank, "Opening a savings account",
                "Open a basic savings account at the branch.",
                List.of("Identity proof (original + copy)", "Address proof (original + copy)", "PAN card or Form 60",
                        "Passport-size photographs"),
                List.of("Download and fill the account opening form, or collect it at the branch",
                        "Keep the initial deposit amount ready if the account type needs one"),
                List.of("Submit the form and documents at the accounts desk", "In-person verification (KYC)",
                        "Collect the welcome kit, or note when it will be sent"),
                List.of("Address on the ID not matching the address you give", "Unsigned photographs or copies"),
                null, "Walk-in", null,
                today.minusDays(60));
        ServiceGuide transcript = guide(uni, "Official transcript request",
                "Request official transcripts for higher studies or job applications.",
                List.of("Student ID or alumni ID", "Copies of all semester mark sheets", "Fee payment receipt", "Filled request form"),
                List.of("Pay the transcript fee (online or at the cash counter)", "Get the request form signed by the department, if required"),
                List.of("Submit the form at the registrar's counter", "Note the collection date or courier tracking details"),
                List.of("Missing mark sheets for some semesters", "Not stating the number of copies or sealed envelopes needed"),
                null, null, "Office hours on working days (check with the office)",
                today.minusDays(110));
        ServiceGuide opd = guide(hospital, "Outpatient (OPD) registration",
                "Register as an outpatient and see a doctor in the general OPD.",
                List.of("Photo ID", "Previous prescriptions and reports, if any"),
                List.of("Find out which OPD and which days apply to your problem"),
                List.of("Register at the OPD registration counter and collect the OP card", "Go to the department and wait for your token",
                        "Consultation; tests or pharmacy as advised"),
                List.of("Going on a day the department's OPD does not run", "Not keeping the OP card for follow-up visits"),
                null, "Walk-in, first come first served", "Morning registration hours (check with the hospital)",
                today.minusDays(200));
        guide(hospital, "Collecting a medical certificate",
                "Get a medical certificate after a consultation.",
                List.of("OP card", "Photo ID"),
                List.of("Consult the doctor first"),
                List.of("Request the certificate from the treating department", "Get it signed and stamped"),
                List.of("Asking at the wrong counter"),
                null, null, null,
                today.minusDays(15));

        contribution(passport, ContributionType.TIP, "Keep a second set of self-attested copies. The counter asked me for an extra copy of the address proof.",
                today.minusDays(12), "Priya", ContributionStatus.APPROVED);
        contribution(passport, ContributionType.TIP, "Security does not allow large bags inside, so travel light.",
                today.minusDays(40), null, ContributionStatus.APPROVED);
        contribution(licence, ContributionType.DISCREPANCY, "On my visit they also asked for a Form 1 self-declaration, which is not listed here.",
                today.minusDays(20), "Karthik", ContributionStatus.APPROVED);
        contribution(account, ContributionType.TIP, "Going on a weekday morning was quick. The whole process took about 30 minutes.",
                today.minusDays(5), null, ContributionStatus.APPROVED);
        contribution(transcript, ContributionType.OUTDATED_WARNING, "I think the fee changed this semester. The receipt amount was different from last year.",
                today.minusDays(3), "Anu", ContributionStatus.PENDING);
        contribution(opd, ContributionType.TIP, "Registration closed earlier than I expected, so go early.",
                today.minusDays(2), null, ContributionStatus.PENDING);

        log.info("Seeded {} institutions and {} guides (sample data)", institutions.count(), guides.count());
    }

    private Institution institution(String name, Category category, String city, String address, String contact, String description) {
        Institution i = new Institution(name, category, city);
        i.setAddress(address);
        i.setContact(contact);
        i.setDescription(description);
        return institutions.save(i);
    }

    private ServiceGuide guide(Institution institution, String title, String purpose,
                               List<String> documents, List<String> prerequisites, List<String> steps, List<String> mistakes,
                               String fees, String appointmentInfo, String timings, LocalDate lastVerifiedAt) {
        ServiceGuide g = new ServiceGuide(institution, title);
        g.setPurpose(purpose);
        g.setDocuments(documents);
        g.setPrerequisites(prerequisites);
        g.setSteps(steps);
        g.setCommonMistakes(mistakes);
        g.setFees(fees);
        g.setAppointmentInfo(appointmentInfo);
        g.setTimings(timings);
        g.setLastVerifiedAt(lastVerifiedAt);
        // verificationStatus stays UNVERIFIED for all sample data
        return guides.save(g);
    }

    private void contribution(ServiceGuide guide, ContributionType type, String text, LocalDate visitDate,
                              String name, ContributionStatus status) {
        Contribution c = new Contribution(guide, type, text, visitDate, name);
        c.setStatus(status);
        if (status == ContributionStatus.APPROVED && type.flagsGuide()) {
            guide.setFlaggedOutdated(true);
        }
        contributions.save(c);
    }
}

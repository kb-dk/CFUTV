package dk.kb.cfutv.service;

import dk.kb.cfutv.GlobalData;
import dk.statsbiblioteket.digitaltv.access.model.*;
import dk.statsbiblioteket.digitaltv.exporter.utils.StringListStringPair;
import dk.statsbiblioteket.generic.utils.FaultException;
import dk.statsbiblioteket.util.Files;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created with IntelliJ IDEA.
 * User: asj
 * Date: 29-08-12
 * Time: 13:07
 * To change this template use File | Settings | File Templates.
 */
public class PBCoreGenerator {
    private final org.slf4j.Logger log = LoggerFactory.getLogger(getClass());

    enum MediaType {
        UNKNOWN, VIDEO, AUDIO, SEVERAL
    }

    public String generateXmlFromTemplate(RitzauProgram ritzauProgram, Boolean tvmeterAvailable){
        if (ritzauProgram == null) {
            log.debug("Entered method generateFileToHotfolderFromTemplate"
                    + " with a null ritzauProgram!");
            return null;
        }
        log.debug("Entered method generateXmlFromTemplate('"
                + "RitzauProgram{" + ritzauProgram.getTitel() + "}')");
        File templateFile;
        String templateWorkingCopy;

        templateFile = new File(GlobalData.getPathToTemplate());

        // Read template
        try {
            templateWorkingCopy = Files.loadString(templateFile);
        } catch (IOException e) {
            throw new FaultException("Could not read template file", e);
        }

        // Fill out template
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_ID]",
                Long.toString(ritzauProgram.getId()) + "RitzauProgram");
        // Though the metadata will be a mix of ritzau and gallup data,
        // the gallup data were found through a compositeprogram found
        // from a ritzau id, so we let the ritzau id be the id of the
        // ritzauProgram.
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_ID_SOURCE]",
                "id");

        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_NUMBER_OF_EPISODES]",
                Long.toString(ritzauProgram.getAntalepisoder()));
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_EPISODE_NUMBER]",
                Long.toString(ritzauProgram.getEpisodenr()));
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_REPEAT_BROADCAST]",
                (ritzauProgram.isGenudsendelse() ? "genudsendelse"
                        : "ikke genudsendelse"));
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_LIVE]",
                (ritzauProgram.isLive() ? "live" : "ikke live"));
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_PRODUCTION_COUNTRY]",
                ritzauProgram.getProduktionsland());
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_PROGRAM_OPHOLD]",
                (ritzauProgram.getProgram_ophold() ? "program ophold"
                        : "ikke program ophold"));
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_AFSNIT_ID]",
                Long.toString(ritzauProgram.getAfsnit_id()));
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_SEASON_ID]",
                Long.toString(ritzauProgram.getSaeson_id()));
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_SERIE_ID]",
                Long.toString(ritzauProgram.getSerie_id()));

        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_PROGRAM_ID]",
                Long.toString(ritzauProgram.getProgram_id()));
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_MAIN_GENRE_ID]",
                Long.toString(ritzauProgram.getHovedgenre_id()));
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_SUB_GENRE_ID]",
                Long.toString(ritzauProgram.getUndergenre_id()));
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_CHANNEL_ID]",
                Integer.toString(ritzauProgram.getKanalId()));
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_PRODUCTION_COUNTRY_ID]",
                Integer.toString(ritzauProgram.getProduktionsland_id()));

        // From query:
        //   digitaltv=> select titel, maintitle from compositeprogram,
        //   ritzauprogram, tvmeterprogram
        //   where ritzauprogram_id=ritzauprogram.id
        //   and tvmeterprogram_originalentry=originalentry;
        // you can see that Ritzau "titel" is usually more informative
        // (and more consistently formatted) than Gallup TVMeter "maintitle".
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_TITLE]",
                ritzauProgram.getTitel());
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_TITLE_TYPE]",
                "titel");
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_ORIGINAL_TITLE]",
                ritzauProgram.getOriginaltitel());
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_TITLE_ORIGINAL]",
                "originaltitel");
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_EPISODE_TITLE]",
                ritzauProgram.getEpisodetitel());
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_TITLE_EPISODE]",
                "episodetitel");
        // Gallup tvmeter "sub title" and "original title" could possibly
        // augment the above.

        // Ritzau descriptions are more informative that Gallup TVMeter desc's
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_SHORT_DESCRIPTION]",
                ritzauProgram.getKortomtale());
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_DESCRIPTION_SHORT]",
                "kortomtale");
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_LONG_DESCRIPTION_1]",
                ritzauProgram.getLangomtale1());
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_DESCRIPTION_LONG_1]",
                "langomtale1");
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_LONG_DESCRIPTION_2]",
                ritzauProgram.getLangomtale2());
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_DESCRIPTION_LONG_2]",
                "langomtale2");

        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_MAIN_GENRE]",
                ritzauProgram.getHovedgenre());
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_SUB_GENRE]",
                ritzauProgram.getUndergenre());
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_TVMETER_CONTENTTYPE]",
                "");

        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_URL_LINK]",
                ritzauProgram.getUrllink());
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_SOUND_LINK]",
                ritzauProgram.getLydlink());

        templateWorkingCopy = insertNormalizedAuthors(ritzauProgram,
                templateWorkingCopy);

        templateWorkingCopy = insertNormalizedContributors(ritzauProgram,
                templateWorkingCopy);

        templateWorkingCopy = insertNormalizedInstruction(ritzauProgram,
                templateWorkingCopy);

        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_CHANNEL_NAME_DK]",
                ritzauProgram.getKanalnavn());
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_CHANNEL_NAME_DK_ROLE]",
                "kanalnavn");

        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_CHANNEL_NAME_ENG]",
                ritzauProgram.getChannel_name());
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_CHANNEL_NAME_ENG_ROLE]",
                "channel_name");

        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_PRODUCTION_YEAR]",
                Long.toString(ritzauProgram.getProduktionsaar()));
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_PREMIERE]",
                (ritzauProgram.isPremiere() ? "premiere" : "ikke premiere"));
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_MEDIA_TYPE]",
                "TODO _ SET MEDIATYPE");//TODO SET MEDIATYPE
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                    "[INSERT_PBC_HD]",
                (ritzauProgram.isHd() ? "hd" : "ikke hd"));
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_PROGRAM_LENGTH]",
                Long.toString(ritzauProgram.getProgramlaengde()));

        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_SURROUND]",
                (ritzauProgram.isSurround() ? "surround" : "ikke surround"));

        if (ritzauProgram.isSekstenni() && !ritzauProgram.isBredformat()) {
            templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                    "[INSERT_PBC_SEKSTENNI_AND_OR_BREDFORMAT]", "16:9");
        } else if (!ritzauProgram.isSekstenni() && ritzauProgram.isBredformat()) {
            templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                    "[INSERT_PBC_SEKSTENNI_AND_OR_BREDFORMAT]", "bredformat");
        } else if (ritzauProgram.isSekstenni() && ritzauProgram.isBredformat()) {
            templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                    "[INSERT_PBC_SEKSTENNI_AND_OR_BREDFORMAT]",
                    "16:9, bredformat");
        } else {
            // !ritzauProgram.isSekstenni() && !ritzauProgram.isBredformat()
            templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                    "[INSERT_PBC_SEKSTENNI_AND_OR_BREDFORMAT]", "");
        }

        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_SH]",
                (ritzauProgram.isSh() ? "sort/hvid" : "farve"));
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_TEXTED]",
                (ritzauProgram.isTekstet() ? "tekstet" : "ikke tekstet"));
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_TH]",
                (ritzauProgram.isTh() ? "tekstet for hørehæmmede"
                        : "ikke tekstet for hørehæmmede"));
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_TTV]",
                (ritzauProgram.isTtv() ? "tekst-tv" : "ikke tekst-tv"));
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_SHOWVIEWCODE]",
                Long.toString(ritzauProgram.getShowviewcode()));

        // This will be Ritzau data (for web-frontend reasons), not TVMeter.
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_START_TIME]",
                formatDate(ZonedDateTime.ofInstant(ritzauProgram.getStarttid().toInstant(), ZoneId.of("Europe/Copenhagen"))));
        // Rather than tvmeterProgram.getStartDate()
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_END_TIME]",
                formatDate(ZonedDateTime.ofInstant(ritzauProgram.getSluttid().toInstant(), ZoneId.of("Europe/Copenhagen"))));
        // Rather than tvmeterProgram.getEndDate()

        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_PBC_ANNOTATION]",
                ritzauProgram.getAnnotation());

        //Is there tvmeter data?
        templateWorkingCopy = replaceEnclosedInCdata(templateWorkingCopy,
                "[INSERT_TVMETER_AVAILABLE]", tvmeterAvailable.toString());

        return templateWorkingCopy;
    }

    /**
     * Encloses the given replacement in CDATA "tags", and replaces every
     * placeholder in template by it. Also makes sure that if the replacement is
     * "null", then the empty string is inserted instead.
     *
     * @param template The string in which to do replacements.
     * @param placeholder The string of which every occurrence should be
     * replaced.
     * @param replacement The string which should be enclosed in CDATA "tags"
     * and inserted at every occurrence of the placeholder.
     * @return The template with every occurrence of the placeholder replaced by
     * the replacement enclosed in CDATA "tags".
     */
    private String replaceEnclosedInCdata(String template,
                                          String placeholder,
                                          String replacement) {
        log.debug("Entered method replaceEnclosedInCdata('"
                + abbreviatedLongText(template)
                + "', '" + placeholder + "', '" + replacement + "')");

        if (replacement == null || replacement.trim().equalsIgnoreCase("null")
                || replacement.trim().equalsIgnoreCase("null.")) {
            replacement = "";
        }

        return replacePlaceholder(template, placeholder,
                encloseStringInCdataForXml(replacement));
    }

    /**
     * Replaces every occurrence of the given placeholder in the given template
     * with the given replacement.
     *
     * @param template The template in which replacements should be made.
     * @param placeholder The placeholder that should be replaced.
     * @param replacement The replacement for occurrences of the placeholder.
     * @return The input template, with every occurrence of the placeholder
     * replaced by the given replacement.
     */
    private String replacePlaceholder(String template,
                                      String placeholder,
                                      String replacement) {
        log.debug("Entered method replacePlaceholder('"
                + abbreviatedLongText(template)
                + "', '" + placeholder + "', '" + replacement + "')");
        String result;
        CharSequence placeholderNonRegex = placeholder;
        CharSequence replacementAsCharSeq;

        if (template == null) {
            result = "";
        } else if (placeholder == null || placeholder.equals("")) {
            result = template;
        } else if (replacement == null) {
            replacementAsCharSeq = "";
            result = template.replace(placeholderNonRegex,
                    replacementAsCharSeq);
        } else {
            replacementAsCharSeq = replacement;
            result = template.replace(placeholderNonRegex,
                    replacementAsCharSeq);
        }
        log.debug("Returning '" + abbreviatedLongText(result) + "'");
        return result;
    }

    /**
     * Enclose the given text in CDATA "tags", after converting any occurrence
     * of "]]>" in the text to "] ] >", and fixing chars that are illegal in
     * xml.
     *
     * @param text The text to be enclosed in CDATA "tags"
     * @return The input text enclosed in CDATA "tags", and with no illegal
     * chars inside.
     */
    private String encloseStringInCdataForXml(String text) {
        String textWithNoBadStuff = replacePlaceholder(text, "]]>", "] ] >");

        String illegalInXML = "["
                + "\u0001-\u0008" + "\u000B-\u000C" + "\u000E-\u001F"
                + "\u007F-\u0084" + "\u0086-\u009F" //+ "\uFDD0-\uFDDF"
//                + "\u1FFFE-\u1FFFF" + "\u2FFFE-\u2FFFF" + "\u3FFFE-\u3FFFF"
//                + "\u4FFFE-\u4FFFF" + "\u5FFFE-\u5FFFF" + "\u6FFFE-\u6FFFF"
//                + "\u7FFFE-\u7FFFF" + "\u8FFFE-\u8FFFF" + "\u9FFFE-\u9FFFF"
//                + "\uAFFFE-\uAFFFF" + "\uBFFFE-\uBFFFF" + "\uCFFFE-\uCFFFF"
//                + "\uDFFFE-\uDFFFF" + "\uEFFFE-\uEFFFF" + "\uFFFFE-\uFFFFF"
//                + "\u10FFFE-\u10FFFF"
                + "]+";

        // Replace illegal characters with space
        textWithNoBadStuff = textWithNoBadStuff.replaceAll(illegalInXML, " ");

        return "<![CDATA[" + textWithNoBadStuff + "]]>";
        //return textWithNoBadStuff;
    }

    /**
     * Abbreviates the given text for logging purposes.
     *
     * @param text The text to be abbreviated.
     * @return The abbreviated text, with length indicator if truncated.
     */
    private static String abbreviatedLongText(String text) {
        if (text == null) {
            text = "";
        }
        return text.length() > 20
                ? (text.substring(0, 20) + "..." + "(length: " + text.length()
                + ")")
                : text;
    }

    /**
     * TODO javadoc
     *
     * @param program
     * @param templateWorkingCopy
     * @return
     */
    private String insertNormalizedAuthors(
            RitzauProgram program, String templateWorkingCopy) {
        String generatedXmlForAuthors = "";
        List<String> normalizedAuthorNames;

        // Gather list of normalized author names
        normalizedAuthorNames = extractNormalizedNames(program.getForfatter());

        if (normalizedAuthorNames.size() == 0) {
            normalizedAuthorNames.add("");
        }

        for (String normalizedAuthorName : normalizedAuthorNames) {
            String authorTemplate = "\n            <pbcoreCreator>\n"
                    + "                <creator>[INSERT_PBC_AUTHOR]</creator>\n"
                    + "                <creatorRole>[INSERT_PBC_AUTHOR_ROLE]"
                    + "</creatorRole>\n"
                    + "            </pbcoreCreator>\n";

            authorTemplate = replaceEnclosedInCdata(authorTemplate,
                    "[INSERT_PBC_AUTHOR]",
                    normalizedAuthorName);
            authorTemplate = replaceEnclosedInCdata(authorTemplate,
                    "[INSERT_PBC_AUTHOR_ROLE]",
                    "forfatter");

            generatedXmlForAuthors += authorTemplate;
        }

        return replacePlaceholder(templateWorkingCopy,
                "[INSERT_PBC_AUTHORS]",
                generatedXmlForAuthors);
    }

    /**
     * TODO javadoc
     *
     * @param program
     * @param templateWorkingCopy
     * @return
     */
    private String insertNormalizedInstruction(
            RitzauProgram program, String templateWorkingCopy) {
        String generatedXmlForInstructors = "";
        List <String> normalizedInstructorNames = new ArrayList<String>();

        // Gather list of normalized instructor names
        normalizedInstructorNames = extractNormalizedNames(
                program.getInstruktion());

        if (normalizedInstructorNames.size() == 0) {
            normalizedInstructorNames.add("");
        }

        for (String normalizedInstructorName : normalizedInstructorNames) {
            String instructorTemplate = "\n            <pbcoreContributor>\n"
                    + "                <contributor>[INSERT_PBC_INSTRUCTION]"
                    + "</contributor>\n"
                    + "                <contributorRole>"
                    + "[INSERT_PBC_INSTRUCTION_ROLE]</contributorRole>\n"
                    + "            </pbcoreContributor>\n";

            instructorTemplate = replaceEnclosedInCdata(instructorTemplate,
                    "[INSERT_PBC_INSTRUCTION]",
                    normalizedInstructorName);
            instructorTemplate = replaceEnclosedInCdata(instructorTemplate,
                    "[INSERT_PBC_INSTRUCTION_ROLE]",
                    "instruktion");

            generatedXmlForInstructors += instructorTemplate;
        }

        return replacePlaceholder(templateWorkingCopy,
                "[INSERT_PBC_INSTRUCTORS]",
                generatedXmlForInstructors);
    }

    /**
     * TODO javadoc
     *
     * @param program
     * @param templateWorkingCopy
     * @return
     */
    private String insertNormalizedContributors(
            RitzauProgram program, String templateWorkingCopy) {
        String generatedXmlForContributors = "";
        List <String> normalizedContributors = new ArrayList<String>();

        // Gather list of normalized contributors
        normalizedContributors = extractNormalizedNames(
                program.getMedvirkende());

        if (normalizedContributors.size() == 0) {
            normalizedContributors.add("");
        }

        for (String normalizedContributor : normalizedContributors) {
            String contributorTemplate = "\n            <pbcoreContributor>\n"
                    + "                <contributor>[INSERT_PBC_CONTRIBUTOR]"
                    + "</contributor>\n"
                    + "                <contributorRole>"
                    + "[INSERT_PBC_CONTRIBUTOR_ROLE]</contributorRole>\n"
                    + "            </pbcoreContributor>\n";

            contributorTemplate = replaceEnclosedInCdata(contributorTemplate,
                    "[INSERT_PBC_CONTRIBUTOR]",
                    normalizedContributor);
            contributorTemplate = replaceEnclosedInCdata(contributorTemplate,
                    "[INSERT_PBC_CONTRIBUTOR_ROLE]",
                    "medvirkende");

            generatedXmlForContributors += contributorTemplate;
        }

        return replacePlaceholder(templateWorkingCopy,
                "[INSERT_PBC_CONTRIBUTORS]",
                generatedXmlForContributors);
    }

    /**
     * Via heuristics, this method splits a String with a list of names from
     * Ritzau. The names are usually separated by commas, except at the end,
     * where "og" is used. Also the ending "." is removed.
     *
     * @param ritzauNames The list of names as a String, in Ritzau's own format
     * @return The list of separated names
     * @throws java.util.regex.PatternSyntaxException In case there is a bug in our pattern for
     * describing the Ritzau format.
     */
    private static List <String> extractNormalizedNames(
            String ritzauNames) throws PatternSyntaxException {
        List <String> names = new ArrayList<String>();
        String toBeSplit = ritzauNames;
        StringListStringPair stringListStringPair;
        String patternDefinition;
        Pattern pattern;
        Matcher matcher;
        boolean matchFound;

        if (ritzauNames == null || ritzauNames.equals("")) {
            return names;
        }

        if (ritzauNames.contains(" efter ")) {
            // For example:
            // forfatter='Abraham Polonsky og Henri Simoun efter roman af Richard Dougherty.'
            // We can't handle everything
            names.add(ritzauNames);
            return names;
        }

        // First, split on "og"
        patternDefinition = "(.*)(\\sog\\s)(.*)[.]";
        pattern = Pattern.compile(patternDefinition);
        matcher = pattern.matcher(toBeSplit);
        matchFound = matcher.find();

        if (matchFound) {
            // "og" was found, split out the last contributor
            names.add(matcher.group(3));

            toBeSplit = matcher.group(1);
            // and continue with the first part
        } else {
            // "og" was not found, probably there is only one name,
            // so toBeSplit is still the same (but we remove a final ".")
            if (toBeSplit.endsWith(".")) {
                toBeSplit = toBeSplit.substring(0, toBeSplit.length() - 1);
            }
        }

        // Perform the rest of the splitting
        stringListStringPair = eatNamesFromList(new StringListStringPair(
                names, toBeSplit));

        names = stringListStringPair.getStrList();
        return names;
    }

    /**
     * TODO javadoc
     *
     * @param stringListStringPair
     * @return
     */
    private static StringListStringPair eatNamesFromList(
            StringListStringPair stringListStringPair) {
        List <String> names = stringListStringPair.getStrList();
        String toBeSplit = stringListStringPair.getStr();

        String patternDefinition;
        Pattern pattern;
        Matcher matcher;
        boolean matchFound;

        if (toBeSplit == "") {
            // We are done, finished recursing
            return stringListStringPair;
        }

        // Split on ", " and call recursively
        patternDefinition = "(.*)(,[\\s]*)([^,]*$)";
        pattern = Pattern.compile(patternDefinition);
        matcher = pattern.matcher(toBeSplit);
        matchFound = matcher.find();

        if (matchFound) {
            names.add(matcher.group(3));
            toBeSplit = matcher.group(1);

            return eatNamesFromList(new StringListStringPair(
                    names, toBeSplit));
        } else {
            // ", " was not found, probably there is only one name left,
            // so we are finished recursing
            names.add(toBeSplit);
            return new StringListStringPair(names, "");
        }
    }

    /**
     * Formats the date to the ISO8601 format.
     *
     * @param date A date
     * @return The date in a string formatted in ISO 8601 format
     */
    private static String formatDate(ZonedDateTime date) {
        SimpleDateFormat ISO8601FORMAT
                = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT);

        return ISO8601FORMAT.format(date);
    }
}

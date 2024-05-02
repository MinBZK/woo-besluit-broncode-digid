
/*
  Deze broncode is openbaar gemaakt vanwege een Woo-verzoek zodat deze
  gericht is op transparantie en niet op hergebruik. Hergebruik van 
  de broncode is toegestaan onder de EUPL licentie, met uitzondering 
  van broncode waarvoor een andere licentie is aangegeven.
  
  Het archief waar dit bestand deel van uitmaakt is te vinden op:
    https://github.com/MinBZK/woo-besluit-broncode-digid
  
  Eventuele kwetsbaarheden kunnen worden gemeld bij het NCSC via:
    https://www.ncsc.nl/contact/kwetsbaarheid-melden
  onder vermelding van "Logius, openbaar gemaakte broncode DigiD" 
  
  Voor overige vragen over dit Woo-besluit kunt u mailen met open@logius.nl
  
  This code has been disclosed in response to a request under the Dutch
  Open Government Act ("Wet open Overheid"). This implies that publication 
  is primarily driven by the need for transparence, not re-use.
  Re-use is permitted under the EUPL-license, with the exception 
  of source files that contain a different license.
  
  The archive that this file originates from can be found at:
    https://github.com/MinBZK/woo-besluit-broncode-digid
  
  Security vulnerabilities may be responsibly disclosed via the Dutch NCSC:
    https://www.ncsc.nl/contact/kwetsbaarheid-melden
  using the reference "Logius, publicly disclosed source code DigiD" 
  
  Other questions regarding this Open Goverment Act decision may be
  directed via email to open@logius.nl
*/

package nl.logius.digid.sharedlib.utils;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public final class VersionUtils {
    private VersionUtils() { }

    /*
     * This method is to be only used in case of jars.
     * it will give the string with the version number, so you can place it in a controller as a reply
     *
     * to get this to work, in the project you ant to use this you need to add the following 3 parts to the pom.xml:
     * 1.:
     *  <plugin>
     *    <groupId>pl.project13.maven</groupId>
     *    <artifactId>git-commit-id-plugin</artifactId>
     *    <version>2.2.4</version>
     *    <executions>
     *      <execution>
     *        <id>get-the-git-infos</id>
     *        <goals>
     *          <goal>revision</goal>
     *        </goals>
     *        <phase>validate</phase>
     *      </execution>
     *    </executions>
     *    <configuration>
     *      <dotGitDirectory>${project.basedir}/.git</dotGitDirectory>
     *    </configuration>
     *  </plugin>
     *
     * 2.:
     *  <finalName>${project.artifactId}-${project.version}-${git.commit.id.abbrev}</finalName>
     *
     * 3.:
     *  <plugin>
     *    <groupId>org.apache.maven.plugins</groupId>
     *    <artifactId>maven-jar-plugin</artifactId>
     *    <configuration>
     *      <archive>
     *        <manifestEntries>
     *          <Implementation-Version>${project.version}-${git.commit.id.abbrev}</Implementation-Version>
     *        </manifestEntries>
     *      </archive>
     *    </configuration>
     *  </plugin>
     *
     * After this its simply to add the method
     */
    public static Map<String,String> getVersionFromJar(Class<?> mainClass) {
        final String version = mainClass.getPackage().getImplementationVersion();
        return version != null ? ImmutableMap.of("version", version) : ImmutableMap.of();
    }
}

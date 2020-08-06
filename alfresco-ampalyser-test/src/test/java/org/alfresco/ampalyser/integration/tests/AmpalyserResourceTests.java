package org.alfresco.ampalyser.integration.tests;

import org.alfresco.ampalyser.AmpalyserClient;
import org.alfresco.ampalyser.models.CommandOutput;
import org.alfresco.ampalyser.util.AppConfig;
import org.alfresco.ampalyser.util.TestResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@ContextConfiguration(classes = AppConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AmpalyserResourceTests extends AbstractTestNGSpringContextTests
{
        @Autowired
        private AmpalyserClient client;

        private CommandOutput cmdOut;

        @Test
        public void testAnalyseFileOverwrite()
        {
                // Run against Alfresco version 6.1.1
                String ampResourcePath = TestResource.getTestResourcePath("analyserTest.amp");
                String version = "6.1.1";
                List<String> cmdOptions = List.of(ampResourcePath, "--target-version=" + version);

                cmdOut = client.runAmpalyserAnalyserCommand(cmdOptions);
                assertEquals(cmdOut.getFileOverwriteTotal(), 3);
                assertTrue(cmdOut.isInFileOverwrite("/images/filetypes/pdf.png"));
                assertTrue(cmdOut.isInFileOverwrite("/images/filetypes/mp4.gif"));
                assertFalse(cmdOut.isInFileOverwrite("/images/filetype/testfile.bmp"));

                // Run against multiple Alfresco versions
                version = "6.0.0-6.2.2";
                List<String> cmdOptions2 = List.of(ampResourcePath, "--target-version=" + version, "--verbose");
                cmdOut = client.runAmpalyserAnalyserCommand(cmdOptions2);

                assertEquals(cmdOut.getFileOverwriteTotal(), 21);
                assertNotNull(cmdOut.retrieveOutputLine("ContextLoaderListener.class,6.0.0-6.2.2", "FILE_OVERWRITE"));
                assertNotNull(cmdOut.retrieveOutputLine("pdf.png,6.0.0-6.2.2", "FILE_OVERWRITE"));
                assertNotNull(cmdOut.retrieveOutputLine("mp4.gif,6.0.0-6.2.2", "FILE_OVERWRITE"));
        }

        @Test
        public void testAnalyseBeanOverwrite()
        {
                // Run against Alfresco version 6.1.1
                String ampResourcePath = TestResource.getTestResourcePath("analyserTest.amp");
                String version = "6.1.1";
                List<String> cmdOptions = List.of(ampResourcePath, "--target-version=" + version);

                cmdOut = client.runAmpalyserAnalyserCommand(cmdOptions);
                System.out.println(cmdOut.getBeanOverwriteConflicts().toString());

                assertEquals(cmdOut.getBeanOverwriteTotal(), 2);
                assertTrue(cmdOut.isInBeanOverwrite("trashcanSchedulerAccessor"));
                assertTrue(cmdOut.isInBeanOverwrite("trashcanCleaner"));

                // Run against multiple Alfresco versions
                version = "6.0.0-6.2.2";
                List<String> cmdOptions2 = List.of(ampResourcePath, "--target-version=" + version, "--verbose");
                cmdOut = client.runAmpalyserAnalyserCommand(cmdOptions2);

                assertEquals(cmdOut.getBeanOverwriteTotal(), 14);
                assertNotNull(cmdOut.retrieveOutputLine("trashcanCleaner,6.0.0-6.2.2", "BEAN"));
                assertNotNull(cmdOut.retrieveOutputLine("trashcanSchedulerAccessor,6.0.0-6.2.2", "BEAN"));
        }

        @Test
        public void testAnalyseAmpPublicAPI()
        {
                // Run against Alfresco version 6.2.2
                String ampResourcePath = TestResource.getTestResourcePath("analyserTest.amp");
                String version = "6.2.2";
                List<String> cmdOptions = List.of(ampResourcePath, "--target-version=" + version);

                cmdOut = client.runAmpalyserAnalyserCommand(cmdOptions);

                assertEquals(cmdOut.getPublicAPITotal(), 2);
                assertTrue(cmdOut.isInPublicAPIConflicts("UseDeprecatedPublicAPI.class"));
                assertTrue(cmdOut.isInPublicAPIConflicts("UseInternalClass.class"));
                assertFalse(cmdOut.isInPublicAPIConflicts("UsePublicAPIClass"));

                // Run against Alfresco version 6.0.0
                version = "6.0.0";
                List<String> cmdOptions1 = List.of(ampResourcePath, "--target-version=" + version);

                cmdOut = client.runAmpalyserAnalyserCommand(cmdOptions1);

                assertEquals(cmdOut.getPublicAPITotal(), 1);
                assertFalse(cmdOut.isInPublicAPIConflicts("UseDeprecatedPublicAPI.class"));
                assertTrue(cmdOut.isInPublicAPIConflicts("UseInternalClass.class"));
                assertFalse(cmdOut.isInPublicAPIConflicts("UsePublicAPIClass.class"));

                // Run against multiple Alfresco versions
                version = "6.0.0-6.2.2";
                List<String> cmdOptions2 = List.of(ampResourcePath, "--target-version=" + version, "--verbose");

                cmdOut = client.runAmpalyserAnalyserCommand(cmdOptions2);

                assertEquals(cmdOut.getPublicAPITotal(), 12);
                assertNotNull(cmdOut.retrieveOutputLine("UseDeprecatedPublicAPI.class,6.1.0-6.2.2", "PUBLIC_API"));
                assertNotNull(cmdOut.retrieveOutputLine("UseInternalClass.class,6.0.0-6.2.2", "PUBLIC_API"));
                assertFalse(cmdOut.isInPublicAPIConflicts("UsePublicAPIClass.class"));
        }

        @Test
        public void testAnalyseJarPublicAPI()
        {
                String ampResourcePath = TestResource.getTestResourcePath("analyserTest.jar");
                String version = "6.2.2";
                List<String> cmdOptions = List.of(ampResourcePath, "--target-version=" + version);

                cmdOut = client.runAmpalyserAnalyserCommand(cmdOptions);

                assertEquals(cmdOut.getPublicAPITotal(), 2);
                assertTrue(cmdOut.isInPublicAPIConflicts("UseDeprecatedPublicAPI.class"));
                assertTrue(cmdOut.isInPublicAPIConflicts("UseInternalClass.class"));
                assertFalse(cmdOut.isInPublicAPIConflicts("UsePublicAPIClass"));

                //Run against multiple Alfresco versions
                version = "6.0.0-6.2.2";
                List<String> cmdOptions2 = List.of(ampResourcePath, "--target-version=" + version, "--verbose");

                cmdOut = client.runAmpalyserAnalyserCommand(cmdOptions2);

                assertEquals(cmdOut.getPublicAPITotal(), 12);
                assertNotNull(cmdOut.retrieveOutputLine("UseDeprecatedPublicAPI.class,6.1.0-6.2.2", "PUBLIC_API"));
                assertNotNull(cmdOut.retrieveOutputLine("UseInternalClass.class,6.0.0-6.2.2", "PUBLIC_API"));
                assertFalse(cmdOut.isInPublicAPIConflicts("UsePublicAPIClass.class"));
        }

        @Test public void testAnalyseClassPathOverwrite()
        {
                // Run against Alfresco version 6.1.1
                String ampResourcePath = TestResource.getTestResourcePath("analyserTest.amp");
                String version = "6.1.1";
                List<String> cmdOptions = List.of(ampResourcePath, "--target-version=" + version);

                cmdOut = client.runAmpalyserAnalyserCommand(cmdOptions);
                assertEquals(cmdOut.getClassPathConflictsTotal(), 1);
                assertTrue(cmdOut.isClassPathConflicts("ContextLoaderListener.class"));

                // Run against multiple Alfresco versions
                version = "6.0.0-6.2.2";
                List<String> cmdOptions2 = List.of(ampResourcePath, "--target-version=" + version, "--verbose");
                cmdOut = client.runAmpalyserAnalyserCommand(cmdOptions2);
                assertEquals(cmdOut.getClassPathConflictsTotal(), 7);
                assertNotNull(cmdOut.retrieveOutputLine("ContextLoaderListener.class,6.0.0-6.2.2", "CLASS_PATH"));
        }

        @Test
        public void testAnalyseThirdPartyLibs()
        {
                String ampResourcePath = TestResource.getTestResourcePath("analyserTest.amp");
                String version = "6.2.2";
                List<String> cmdOptions = List.of(ampResourcePath, "--target-version=" + version);

                cmdOut = client.runAmpalyserAnalyserCommand(cmdOptions);

                assertEquals(cmdOut.getThirdPartyLibTotal(), 1);
                assertTrue(cmdOut.isInThirdPartyLibConflicts("ThirdPartyLibs.class"));
                assertFalse(cmdOut.isInThirdPartyLibConflicts("AccessControlList.class"));
                assertFalse(cmdOut.isInThirdPartyLibConflicts("OtherThirdPartyLibs.class"));

                // Run against multiple Alfresco versions
                version = "6.0.0-6.2.2";
                List<String> cmdOptions2 = List.of(ampResourcePath, "--target-version=" + version, "--verbose");
                cmdOut = client.runAmpalyserAnalyserCommand(cmdOptions2);
                assertEquals(cmdOut.getClassPathConflictsTotal(), 7);
                assertNotNull(cmdOut.retrieveOutputLine("ThirdPartyLibs.class,6.0.0-6.2.2", "3RD_PARTY_LIBS"));
        }
}

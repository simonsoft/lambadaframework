package org.lambadaframework.jaxrs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;
import org.lambadaframework.jaxrs.model.Resource;
import org.lambadaframework.stubs.StubHandler;

public class JAXRSParserTest {

    /**
     * To run this test, lambada-stub-handlers module should be compiled and packaged in lambada-stub-handlers/target folder
     */
    @Test
    public void testScanJar() throws Exception {

        // Finds the filename of the stub-handler-jar without regards to version numbering.
        String stubHandlerFileName = new File("../stub-handlers/target").listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith("stub-handlers-");
            }
        })[0].getAbsolutePath();
        JAXRSParser parser = new JAXRSParser().withJarFile(stubHandlerFileName, "org.lambadaframework");
        List<Resource> resourceList = parser.scan();
        assertFalse(resourceList.isEmpty());
    }

    @Test
    public void testScanPackage() throws Exception {
        JAXRSParser parser = new JAXRSParser().withPackageName("org.lambadaframework", StubHandler.class);
        List<Resource> resourceList = parser.scan();
        assertEquals(4, resourceList.size());
        int totalMethod = 0;
        for (Resource resource : resourceList) {
            totalMethod += resource.getResourceMethods().size();
        }
        assertEquals(6, totalMethod);

        Collections.sort(resourceList, new PathComparator());

        assertEquals("/resource1", resourceList.get(0).getPath());
        assertEquals("/resource1", resourceList.get(1).getPath());
        assertEquals("/resource1/{id}", resourceList.get(2).getPath());
        assertEquals("/resource1/{id}/users", resourceList.get(3).getPath());
    }

    private class PathComparator implements Comparator<Resource> {

        @Override
        public int compare(Resource o1, Resource o2) {
            return o1.getPath().compareTo(o2.getPath());
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }
}
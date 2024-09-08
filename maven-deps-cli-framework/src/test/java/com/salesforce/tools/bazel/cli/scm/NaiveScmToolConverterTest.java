package com.salesforce.tools.bazel.cli.scm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.salesforce.tools.bazel.cli.scm.NaiveScmTool.Perforce;

public class NaiveScmToolConverterTest {

    @Test
    void p4WithClientSpecAndChangelist() throws Exception {
        var tool = new NaiveScmToolConverter().convert("p4:server-whatever123:1323");

        assertNotNull(tool);
        assertTrue(tool instanceof NaiveScmTool.Perforce);

        var perforceTool = (Perforce) tool;
        assertEquals("server-whatever123", perforceTool.clientSpec);
        assertEquals("1323", perforceTool.changeList);

    }

}

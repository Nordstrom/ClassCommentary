package painpoint.domain.util;

import org.junit.Test;
import painpoint.domain.util.DataModelUtil;

import static org.junit.Assert.*;

public class DataModelUtilTest {

    @Test
    public void testClassId_pathToDirectory() throws Exception {

        //GIVEN Valid filePath, className, and ProjectName.
        String fileName = "FileManager.java";
        String path = "/Users/ProjectName/app/src/fun"; //<-- no trailing slash.
        Integer ExpectedId = ("/ProjectName/app/src/fun/FileManager.java").hashCode();

        // WHEN DataModelUtil.commentaryId is called with the valid params.
        Integer commentaryId = DataModelUtil.generateClassFileId(fileName, path, "ProjectName");

        //THEN the CommentaryModel has the expected Result
        assertEquals(commentaryId, ExpectedId);
    }

    @Test
    public void testClassId_pathToDirectory_trailingSlash() throws Exception {

        //GIVEN Valid filePath, className, and ProjectName.
        String fileName = "FileManager.java";
        String path = "/Users/ProjectName/app/src/fun/"; // <--- The trailing slash
        Integer ExpectedId = ("/ProjectName/app/src/fun/FileManager.java").hashCode();

        // WHEN DataModelUtil.commentaryId is called with the valid params.
        Integer commentaryId = DataModelUtil.generateClassFileId(fileName, path, "ProjectName");

        //THEN the CommentaryModel has the expected Result
        assertEquals(commentaryId, ExpectedId);
    }

    @Test
    public void testClassId_pathToFile() throws Exception {

        //GIVEN Valid filePath, className, and ProjectName.
        String fileName = "FileManager.java";
        String path = "/Users/ProjectName/app/src/fun/FileManager.java";
        Integer ExpectedId = ("/ProjectName/app/src/fun/FileManager.java").hashCode();

        // WHEN DataModelUtil.commentaryId is called with the valid params.
        Integer commentaryId = DataModelUtil.generateClassFileId(fileName, path, "ProjectName");

        //THEN the CommentaryModel has the expected Result
        assertEquals(commentaryId, ExpectedId);
    }



}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.whitewashing.php.cs;

import java.util.concurrent.ExecutionException;
import org.netbeans.modules.php.project.util.PhpProgram;
import java.io.File;
import java.util.concurrent.Future;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.extexecution.ExternalProcessBuilder;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.loaders.DataObject;
import org.openide.text.Line;

/**
 *
 * @author benny
 */
public class CodeSniffer extends PhpProgram {

    public static final String PARAM_STANDARD = "--standard=Zend";
    public static final String PARAM_REPORT = "--report=xml";

    public CodeSnifferOutput output = null;

    public CodeSniffer(String command) {
        super(command);
    }

    CodeSnifferXmlLogResult execute(FileObject fo) {
        final File parent = FileUtil.toFile(fo.getParent());

        ExternalProcessBuilder externalProcessBuilder = new ExternalProcessBuilder(this.getProgram())
                .workingDirectory(parent)
                .addArgument(PARAM_REPORT)
                .addArgument(fo.getNameExt());

        this.output = new CodeSnifferOutput();


        ExecutionDescriptor descriptor = new ExecutionDescriptor()
                .frontWindow(false)
                .controllable(false)
                .outProcessorFactory(this.output);

        ExecutionService service = ExecutionService.newService(externalProcessBuilder,
                descriptor, "PHP Coding Standards");


        Future<Integer> task = service.run();
        try {
            task.get();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }

        return annotateWithCodingStandardHints(fo);
    }

    private CodeSnifferXmlLogResult annotateWithCodingStandardHints(FileObject fo)
    {
        CodeSnifferXmlLogParser parser = new CodeSnifferXmlLogParser();
        CodeSnifferXmlLogResult rs = parser.parse(this.output.getReader());

        try {
            DataObject d = DataObject.find(fo);
            
            LineCookie cookie = (LineCookie)d.getCookie(LineCookie.class);

            Line.Set lineSet = null;
            Line line = null;
            for(int i = 0; i < rs.getCsErrors().size(); i++) {
                lineSet = cookie.getLineSet();
                line = lineSet.getOriginal(rs.getCsErrors().get(i).getLineNum());
                rs.getCsErrors().get(i).attach(line);
            }

            for(int i = 0; i < rs.getCsWarnings().size(); i++) {
                lineSet = cookie.getLineSet();
                line = lineSet.getOriginal(rs.getCsWarnings().get(i).getLineNum());
                rs.getCsWarnings().get(i).attach(line);
            }
        } catch (DataObjectNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }

        return rs;
    }
}

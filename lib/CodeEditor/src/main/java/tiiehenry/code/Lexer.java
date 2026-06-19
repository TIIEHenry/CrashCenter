package tiiehenry.code;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tiiehenry.code.doc.DocumentProvider;
import tiiehenry.code.language.text.TextLanguage;
import tiiehenry.code.praser.Span;

public class Lexer {

    private static final ExecutorService mExecutorService;

    static {
        mExecutorService = Executors.newSingleThreadExecutor();
    }

    private final LexCallback _callback;
    private LexTask _workerTask = TextLanguage.defaultLexTask;

    public Lexer(LexCallback callback, LexTask workerTask) {
        _callback = callback;
        setWorkerTask(workerTask);
    }


    public void setWorkerTask(LexTask workerTask) {
        _workerTask = workerTask;
        workerTask.setLexer(this);
    }

    public LexTask getWorkerTask() {
        return _workerTask;
    }



    public void tokenize(DocumentProvider hDoc) {
        if (_workerTask != null) {
            LexTask task = this._workerTask;
            //tokenize will modify the state of hDoc; make a copy
            task.abort();
//            task.se
            task.setDocumentProvider(new DocumentProvider(hDoc));
            mExecutorService.execute(task);
        }
    }

    void tokenizeDone(List<Span> result) {
        if (_callback != null) {
            _callback.lexDone(result);
        }
    }

    public void cancelTokenize() {
        if (_workerTask != null) {
            _workerTask.abort();
        }
    }

    public interface LexCallback {
        void lexDone(List<Span> results);
    }

}

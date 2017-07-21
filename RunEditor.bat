@echo off
java -Xmx6G -Xms6G -Djava.library.path=natives -Dresource_dir=rsrc -cp "buildlibs/*" com.threerings.bang.editor.EditorDesktop
pause

package eval;

import io.anuke.arc.*;
import io.anuke.arc.function.*;
import io.anuke.arc.util.async.*;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.Files;

public class Eval{
    private static AsyncExecutor exec = new AsyncExecutor(1);
    private static final String className = "__eval0__";

    private static JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private static String[] imports = {"java.util", "io.anuke.mindustry", "io.anuke.mindustry.game", "io.anuke.mindustry.type", "io.anuke.mindustry.core", "io.anuke.mindustry.content", "io.anuke.arc", "io.anuke.arc.util", "io.anuke.arc.function"};

    static void eval(String ccode, Consumer<String> callback, Consumer<Throwable> error){

        exec.submit(() -> {
            try{
                if(compiler == null){
                    error.accept(new RuntimeException("Your Java installation does not support Eval, specifically ToolProvider.getSystemJavaCompiler()"));
                    return;
                }
                String code = (ccode.startsWith(";") ? ccode.substring(1) : "callback__.accept(String.valueOf(" + ccode + ")  );") + ";";

                invoke(className, io.anuke.arc.collection.Array.with(imports).map(f -> "import " + f + ".*;").toString("\n") + "\n" + "\nimport static io.anuke.mindustry.Vars.*;\npublic class "
                    + className + "{ public static Consumer<String> callback__; public static Consumer<Throwable> error__; public static void test() {try{" + code + "}catch (Throwable e){ error__.accept(e);}} }", s -> Core.app.post(() -> callback.accept(s)), s -> Core.app.post(() -> error.accept(s)));
            }catch(Throwable e){
                e.printStackTrace();
                error.accept(new Exception(e));
            }
        });
    }

    static void invoke(String filename, String source, Consumer<String> callback, Consumer<Throwable> error) throws Exception{
        File root = Core.settings.getDataDirectory().child("tmp/").file();
        File sourceFile = new File(root, filename + ".java");
        sourceFile.getParentFile().mkdirs();
        Files.write(sourceFile.toPath(), source.getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream err = new ByteArrayOutputStream();

        compiler.run(null, null, err, sourceFile.getPath());

        String str = new String(err.toByteArray());
        if(!str.isEmpty()){
            if(str.contains("'void' type not allowed here") && source.contains("callback__.accept(String.valueOf(")){
                invoke(filename, source.replace(")  )", "").replace("error__.accept(String.valueOf(", ""), callback, error);
            }else{
                error.accept(new RuntimeException(str));
            }
            return;
        }

        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { root.toURI().toURL()});
        Class<?> cls = Class.forName(filename, true, classLoader);
        cls.getField("callback__").set(null, callback);
        cls.getField("error__").set(null, error);
        Method method = cls.getMethod("test");
        Core.app.post(() -> {
            try{
                method.invoke(null);
            }catch(Throwable t){
                error.accept(t);
            }
        });
    }
}
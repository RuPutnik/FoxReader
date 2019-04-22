package ru.putnik.foxreader.controller;

import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import ru.putnik.foxreader.model.RequestModel;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Создано 15.04.2019 в 14:33
 */
public class RequestController implements Initializable {
    private RequestModel model=new RequestModel();
    private static final String PATH_FXML= "view/RequestView.fxml";
    private static MainController controller;
    private static String oldReq;
    private static String request="";
    private static Stage stage;
    private int i;

    private static final String[] KEYWORDS = new String[] {
            "add", "all", "alter", "and", "any","as", "asc", "authorization", "backup", "begin",
            "between", "break", "browse", "bulk", "by","cascade", "case", "check", "checkpoint", "close",
            "clustered", "coalesce", "collate", "column", "commit","compute", "constraint",
            "continue", "convert", "create","cross", "current", "current_date", "current_time",
            "current_timestamp","current_user", "cursor", "database", "dbcc", "deallocate",
            "declare", "default", "delete", "deny", "desc","disc", "distinct", "distributed", "double",
            "drop","dump", "else", "end", "errlvl", "escape","except", "exec", "execute", "exists", "exit",
            "external", "fetch", "file", "fillfactor", "for","foreign", "freetext", "freetexttable", "from", "full",
            "function", "goto", "grant", "group", "having","holdlock", "identity", "identity_insert",
            "identitycol", "if","in", "index", "inner", "insert", "intersect","into", "is",
            "join", "key", "kill","left", "like", "lineno", "load", "merge","national", "nocheck",
            "nonclustered", "not", "null","nullif", "of", "off", "offsets", "on", "open",
            "opendatasource", "openquery", "openrowset", "openxml","option", "or", "order", "outer", "over",
            "percent", "pivot", "plan", "precision", "primary", "print", "proc", "procedure", "public", "raiserror",
            "read", "readtext", "reconfigure", "references", "replication","restore", "restrict", "return",
            "revert", "revoke","right", "rollback", "rowcount", "rowguidcol", "rule","save", "schema",
            "securityaudit", "select", "semantickeyphrasetable","semanticsimularitydetailstable",
            "semanticsimularitytable", "session_user", "set", "setuser","shutdown", "some", "statistics",
            "system_user", "table","tablesample", "textsize", "then", "to", "tran","transaction", "trigger",
            "truncate", "try_convert", "tsequal","union", "unique", "unpivot", "update", "updatetext",
            "use", "values", "varying", "view", "waitfor","when", "where", "while", "withingroup", "writetext"
    };
    RequestController(ArrayList<String> listNames){
        String[] NAMES = listNames.toArray(new String[listNames.size()]);
        String NAMES_PATTERN = "\\b(" + String.join("|", NAMES) + ")\\b";
        PATTERN = Pattern.compile(
                           "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                        + "|(?<NAMES>" + NAMES_PATTERN + ")"
                        + "|(?<PAREN>" + PAREN_PATTERN + ")"
                        + "|(?<BRACE>" + BRACE_PATTERN + ")"
                        + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                        + "|(?<STRING>" + STRING_PATTERN + ")"
                        + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                        + "|(?<SIGNS>" + SIGNS + ")");
    }
    public RequestController(){}

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "[()]";
    private static final String BRACE_PATTERN = "[{}]";
    private static final String SEMICOLON_PATTERN = ";";
    private static final String SIGNS="[*<>=!]";
    private static final String STRING_PATTERN = "\'([^\"\\\\]|\\\\.)*\'";
    private static final String COMMENT_PATTERN = "<!--([^\"\\\\]|\\\\.)*-->";

    private static Pattern PATTERN;

    @FXML
    public CodeArea sqlReqArea;
    @FXML
    public Button handleRequestButton;
    @FXML
    public Button clearArea;
    @FXML
    public Button handleAndCloseButton;
    @FXML
    public Button saveButton;
    @FXML
    public Button notSaveButton;

    private static ExecutorService executor;

    String showView(MainController controller, String oldRequest){
        RequestController.controller=controller;
        oldReq=oldRequest;
        try {
            stage=new Stage();
            Parent parent=FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource(PATH_FXML)));
            Scene scene=new Scene(parent);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getClassLoader().getResource("sql-styles.css")).toExternalForm());
            stage.setScene(scene);
            stage.getIcons().add(new Image("icons/foxIcon.png"));
            stage.setTitle("Окно генерации");
            stage.setResizable(false);

            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return request;
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sqlReqArea.appendText(oldReq);
        sqlReqArea.setAutoScrollOnDragDesired(true);
        sqlReqArea.setParagraphGraphicFactory(LineNumberFactory.get(sqlReqArea));
        sqlReqArea.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        executor = Executors.newSingleThreadExecutor();
        autocomplete(sqlReqArea);
        ContextMenu menu=new ContextMenu();
        MenuItem item=new MenuItem("Показать план выполнения");
        item.setOnAction(event -> {
            model.printPlaneExecute(sqlReqArea.getText());
        });
        menu.getItems().add(item);
        sqlReqArea.setContextMenu(menu);

        handleRequestButton.setOnAction(event -> {
            request=sqlReqArea.getText();
            controller.sendRequest(request);
        });
        clearArea.setOnAction(event -> sqlReqArea.clear());
        handleAndCloseButton.setOnAction(event -> {
            request=sqlReqArea.getText();
            controller.sendRequest(request);
            stage.close();
        });
        saveButton.setOnAction(event -> {
            request=sqlReqArea.getText();
            stage.close();
        });
        notSaveButton.setOnAction(event -> {
            request=oldReq;
            stage.close();
        });
        stage.setOnCloseRequest(event -> {
            request=oldReq;
            stage.close();
        });
        //Если нужно отключить выделение
        sqlReqArea.multiPlainChanges().successionEnds(Duration.ofMillis(500)).supplyTask(this::computeHighlightingAsync)
                .awaitLatest(sqlReqArea.multiPlainChanges()).filterMap(t -> {
                    if(t.isSuccess()) {
                        return Optional.of(t.get());
                    } else {
                        t.getFailure().printStackTrace();
                        return Optional.empty();
                    }
                }).subscribe(this::applyHighlighting);

    }
    private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
        String text = sqlReqArea.getText();
        Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
            @Override
            protected StyleSpans<Collection<String>> call(){
                return computeHighlighting(text);
            }
        };
        executor.execute(task);
        return task;
    }
    private void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
        sqlReqArea.setStyleSpans(0, highlighting);
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text.toLowerCase());

        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder= new StyleSpansBuilder<>();
        while(matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                            matcher.group("NAMES") != null ? "names" :
                                matcher.group("PAREN") != null ? "paren" :
                                        matcher.group("BRACE") != null ? "brace" :
                                                matcher.group("SEMICOLON") != null ? "semicolon" :
                                                    matcher.group("STRING") != null ? "string" :
                                                            matcher.group("COMMENT") != null ? "comment" :
                                                                    matcher.group("SIGNS") != null ? "signs" :
                                                                    null; /* never happens */ assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
    private void autocomplete(CodeArea area) {
        try {
            ContextMenu menu = new ContextMenu();
            EventHandler<KeyEvent> handler = event -> {
                if (event.getCode() != KeyCode.SPACE) {
                    if (area.getText().contains(" ")) {
                        if (i == 0) {
                            i = area.getCaretColumn();
                        }
                        if (i < 0) i = 0;
                        String text = area.getText() + event.getText();
                        text = text.toLowerCase();
                        if ((text.split(" ")[text.split(" ").length - 1]).length() > 2) {

                            menu.getItems().clear();
                            text = (text.split(" ")[text.split(" ").length - 1]);
                            for (int a = 0; a < KEYWORDS.length; a++) {
                                int b = a;
                                if (KEYWORDS[a].contains(text)) {
                                    MenuItem item = new MenuItem(KEYWORDS[a]);
                                    item.setOnAction(event1 -> {
                                        area.replace(i, area.getCaretColumn(), KEYWORDS[b].toUpperCase(), new ArrayList<>());
                                    });
                                    menu.getItems().add(item);
                                }
                            }
                            if (menu.getItems().size() > 0) {
                                menu.show(area, Side.RIGHT, 0, 0);
                            }
                        }
                    } else {
                        if (area.getText().length() > 2) {
                            menu.getItems().clear();
                            for (int a = 0; a < KEYWORDS.length; a++) {
                                int b = a;
                                if (KEYWORDS[a].contains(area.getText())) {
                                    MenuItem item = new MenuItem(KEYWORDS[a]);
                                    item.setOnAction(event1 -> {
                                        if (i < 0) i = 0;
                                        area.replace(i, area.getCaretColumn(), KEYWORDS[b].toUpperCase(), new ArrayList<>());
                                    });
                                    menu.getItems().add(item);
                                }
                            }
                            if (menu.getItems().size() > 0) {
                                menu.show(area, Side.LEFT, 0, 0);
                            }
                        }
                    }
                }
                if (event.getCode() == KeyCode.SPACE) {
                    i = 0;
                }
                if (event.getCode() == KeyCode.BACK_SPACE) {
                    i = area.getCaretColumn() - 1;
                }
            };
            EventHandler<KeyEvent> handler1 = event -> {
                if (event.getText().equals("'") || ((event.getCode() == KeyCode.DIGIT9 && event.isShiftDown())) || (event.getCode() == KeyCode.OPEN_BRACKET && event.isShiftDown())) {
                    if (event.getText().equals("'")) {
                        area.appendText(event.getText());
                    } else if (event.getCode() == KeyCode.DIGIT9 && event.isShiftDown()) {
                        area.appendText(")");
                    } else if (event.getCode() == KeyCode.OPEN_BRACKET && event.isShiftDown()) {
                        area.appendText("}");
                    }
                }
            };
            area.addEventFilter(KeyEvent.KEY_PRESSED, handler);
            area.addEventFilter(KeyEvent.KEY_RELEASED, handler1);
        } catch (Exception ignored) {}
    }
}

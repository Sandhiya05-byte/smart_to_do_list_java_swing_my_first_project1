// Smart AI To-Do List App with ChatGPT Suggestion + Tree Growth Visualization
// Required: OkHttp, JSON, and Okio JARs added to project dependencies

import javax.swing.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

import okhttp3.*;
import org.json.*;

public class ToDoList {
    private JFrame frame;
    private DefaultListModel<Task> taskListModel;
    private DefaultListModel<Task> completedListModel;
    private JList<Task> taskList;
    private JList<Task> completedList;
    private JTextField taskInput;
    private JTextField dueDateInput;
    private JComboBox<String> categoryCombo;
    private TreePanel treePanel;
    private JLabel suggestionLabel;

    public ToDoList() {
        frame = new JFrame("\u2728 Smart AI To-Do List App \u2728");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLayout(new BorderLayout());

        Font font = new Font("Arial", Font.BOLD, 14);
        taskInput = new JTextField(15);
        dueDateInput = new JTextField(10);
        taskInput.setFont(font);
        dueDateInput.setFont(font);

        String[] categories = {"Skin Care", "Shopping", "Study", "Personal", "Work"};
        categoryCombo = new JComboBox<>(categories);
        categoryCombo.setFont(font);

        taskListModel = new DefaultListModel<>();
        taskList = new JList<>(taskListModel);
        taskList.setFont(font);

        completedListModel = new DefaultListModel<>();
        completedList = new JList<>(completedListModel);
        completedList.setFont(font);

        JButton addButton = new JButton("➕ Add Task");
        JButton deleteButton = new JButton("❌ Delete");
        JButton doneButton = new JButton("✅ Mark as Done");
        JButton aiSuggestButton = new JButton("💡 Suggest AI Task");

        addButton.setFont(font);
        deleteButton.setFont(font);
        doneButton.setFont(font);
        aiSuggestButton.setFont(font);
        aiSuggestButton.setBackground(new Color(200, 230, 250));

        addButton.addActionListener(e -> {
            String task = taskInput.getText().trim();
            String dueDate = dueDateInput.getText().trim();
            String category = (String) categoryCombo.getSelectedItem();

            if (!task.isEmpty() && !dueDate.isEmpty()) {
                Task newTask = new Task(category, task, dueDate);
                taskListModel.addElement(newTask);
                sortTaskListModel();
                taskInput.setText("");
                dueDateInput.setText("");
                updateSuggestion();
            } else {
                JOptionPane.showMessageDialog(frame, "Please fill all fields.");
            }
        });

        aiSuggestButton.addActionListener(e -> {
            String category = (String) categoryCombo.getSelectedItem();
            String aiTask = ChatGPTClient.generateOfflineTask(category);  // ✅ Use offline suggestion
            taskInput.setText(aiTask);
        });


        deleteButton.addActionListener(e -> {
            int selectedIndex = taskList.getSelectedIndex();
            if (selectedIndex != -1) {
                taskListModel.remove(selectedIndex);
                updateSuggestion();
            }
        });

        doneButton.addActionListener(e -> {
            int selectedIndex = taskList.getSelectedIndex();
            if (selectedIndex != -1) {
                Task completed = taskListModel.get(selectedIndex);
                taskListModel.remove(selectedIndex);
                completedListModel.addElement(completed);
                JOptionPane.showMessageDialog(frame, "Great job! 🎉 Task Completed!");
                playCelebrationSound();
                treePanel.incrementLeaves();
                updateSuggestion();
            }
        });

        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("Task:"));
        inputPanel.add(taskInput);
        inputPanel.add(new JLabel("Due Date (yyyy-MM-dd):"));
        inputPanel.add(dueDateInput);
        inputPanel.add(new JLabel("Category:"));
        inputPanel.add(categoryCombo);
        inputPanel.add(addButton);
        inputPanel.add(aiSuggestButton);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(deleteButton);
        buttonPanel.add(doneButton);

        JPanel listPanel = new JPanel(new GridLayout(1, 2));
        listPanel.add(new JScrollPane(taskList));
        listPanel.add(new JScrollPane(completedList));

        treePanel = new TreePanel();
        treePanel.setPreferredSize(new Dimension(200, 600));

        suggestionLabel = new JLabel("👉 Suggested Task: None", SwingConstants.CENTER);
        suggestionLabel.setFont(new Font("Arial", Font.ITALIC, 16));

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(buttonPanel, BorderLayout.CENTER);
        frame.add(listPanel, BorderLayout.SOUTH);
        frame.add(treePanel, BorderLayout.EAST);
        frame.add(suggestionLabel, BorderLayout.WEST);

        frame.setVisible(true);
    }

    private void updateSuggestion() {
        if (taskListModel.isEmpty()) {
            suggestionLabel.setText("👉 Suggested Task: None");
        } else {
            Task top = taskListModel.get(0);
            suggestionLabel.setText("👉 Suggested Task: " + top);
        }
    }

    private void playCelebrationSound() {
        try {
            URL soundURL = getClass().getClassLoader().getResource("hurray.wav");
            if (soundURL == null) return;
            AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundURL);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInput);
            clip.start();
        } catch (Exception e) {
            System.out.println("Sound Error: " + e.getMessage());
        }
    }

    private void sortTaskListModel() {
        List<Task> tempList = new ArrayList<>();
        for (int i = 0; i < taskListModel.size(); i++) {
            tempList.add(taskListModel.get(i));
        }
        tempList.sort(Comparator.comparing(Task::getDueDate));
        taskListModel.clear();
        for (Task task : tempList) {
            taskListModel.addElement(task);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ToDoList::new);
    }
}

class Task {
    private String category;
    private String title;
    LocalDate dueDate;

    public Task(String category, String title, String dueDate) {
        this.category = category;
        this.title = title;
        try {
            this.dueDate = LocalDate.parse(dueDate);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(null,
                    "Invalid date format! Please use yyyy-MM-dd (e.g. 2025-06-04)",
                    "Date Format Error", JOptionPane.ERROR_MESSAGE);
            this.dueDate = LocalDate.now(); // fallback date
        }
    }



    public LocalDate getDueDate() {
        return dueDate;
    }

    public String toString() {
        return category + " | " + title + " | Due: " + dueDate;
    }
}

class TreePanel extends JPanel {
    private int completedTasks = 0;

    public void incrementLeaves() {
        completedTasks++;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        URL imgURL = getClass().getClassLoader().getResource("tree_base.png");
        if (imgURL != null) {
            ImageIcon treeImage = new ImageIcon(imgURL);
            treeImage.paintIcon(this, g, 20, 20);
        }

        g.setColor(Color.GREEN);
        for (int i = 0; i < completedTasks; i++) {
            g.fillOval(50 + (i % 5) * 15, 200 - (i / 5) * 20, 10, 10);
        }
    }
}

class ChatGPTClient {
    private static final Map<String, List<String>> suggestions = new HashMap<>();

    static {
        suggestions.put("Skin Care", Arrays.asList(
                "Apply aloe vera gel before sleep",
                "Drink 2 extra glasses of water today",
                "Try a homemade face mask with turmeric and curd",
                "Gently exfoliate your skin today",
                "Massage your face with coconut oil for 5 mins"
        ));
        suggestions.put("Study", Arrays.asList(
                "Revise DBMS Normal Forms",
                "Solve 3 LeetCode problems",
                "Summarize one chapter in 10 bullet points",
                "Watch one YouTube lecture and make notes",
                "Create a mind map for DSA topics"
        ));
        suggestions.put("Shopping", Arrays.asList(
                "Make a budget grocery list",
                "Clean your shopping bag",
                "Compare prices for your wishlist items",
                "Add one healthy snack to your shopping list",
                "Buy refill packs to save money"
        ));
        suggestions.put("Personal", Arrays.asList(
                "Journal your day for 5 minutes",
                "Call someone you miss",
                "Declutter one drawer today",
                "Go for a short walk or stretch",
                "Meditate for 10 minutes"
        ));
        suggestions.put("Work", Arrays.asList(
                "Organize your desktop files",
                "Write down 3 work goals for the week",
                "Clear 5 emails from inbox",
                "Prepare a to-do list for tomorrow",
                "Review your last project notes"
        ));
    }

    public static String generateOfflineTask(String category) {
        List<String> tasks = suggestions.getOrDefault(category, Arrays.asList("Take a deep breath and smile 😊"));
        Random rand = new Random();
        return tasks.get(rand.nextInt(tasks.size()));
    }
}

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.awt.Font;

public class JSecretPhrase extends JFrame
{
   private static final int MAX_MISTAKES = 6;
   private static final String DEFAULT_PHRASE = "its a feature";
   private static final String PHRASE_FILE = "data\\phrases.txt";
   private static final char PLACEHOLDER = '*';
   private static final int LINE_END_CHAR_COUNT = 2;
   private static final String[] END_TEXT = 
   {
      "Congrats! Perfect game!",
      "Congrats! Amazing job!",
      "Congrats! Great job!",
      "Congrats! Good job!",
      "Congrats! You won!",
      "Congrats! It was a close game.",
      "Try again next time!"
   };
   private static final Color[] COLORS = 
   {
      Color.RED,
      Color.BLUE,
      Color.GREEN,
      Color.BLACK,
      new Color(156, 156, 0), //Darker yellow
      Color.ORANGE
   };
   
   private static int headerSize;
   private static int phraseCount;
   private static int entryLength;
   
   private int mistakes;
   private int correct;
   private int letterCount;
   private Color manColor;
   private String phrase;
   private StringBuilder outputText;
   private KeyboardLayout layout;
   
   private JButton[] letters;
   private JLabel lbl_output;
   
   public static void main(String[] args)
   {
      Object[] choices = KeyboardLayout.values();
      
      //Loops until the player picks an option
      int result = 0;
      do
      {
         result = JOptionPane.showOptionDialog(null,
               "Please select your keyboard layout",
               "Keyboard select",
               JOptionPane.YES_NO_CANCEL_OPTION,
               JOptionPane.QUESTION_MESSAGE,
               null,
               choices,
               choices[0]);
      } while (result == JOptionPane.CLOSED_OPTION);
      
      JSecretPhrase game = new JSecretPhrase((KeyboardLayout)choices[result]);
      
      game.setVisible(true);
      game.repaint();
   }
   
 //Loads a phrase from file.
   public static String loadPhrase()
   {
      String absolutePath = FileSystems.getDefault()
            .getPath(PHRASE_FILE)
            .normalize()
            .toAbsolutePath()
            .toString();


      Path path = Paths.get(absolutePath);
      FileChannel channel;
      byte[] data;
      ByteBuffer buffer;
      String phrase = "";
      
      try
      {
         channel = (FileChannel)Files.newByteChannel(path);
         
         //Checks if the file format info has been loaded 
         if (headerSize == -1)
         {
            data = new byte[10];
            buffer = ByteBuffer.wrap(data);
            channel.read(buffer);
            
            String line = new String(data);
            String header = line.substring(0, line.indexOf('\r'));
            String[] split = header.split("[,]");
            
            headerSize = header.length();
            phraseCount = Integer.parseInt(split[0]);
            entryLength = Integer.parseInt(split[1]);
         }
         
         data = new byte[entryLength];
         buffer = ByteBuffer.wrap(data);
         
         int index = (int)(Math.random() * phraseCount);
         
         channel.position(index * (entryLength + LINE_END_CHAR_COUNT) + 
               headerSize + LINE_END_CHAR_COUNT);
         channel.read(buffer);
         phrase = new String(data).trim();
      }
      catch (Exception e)
      {
         System.out.println("There was an error loading the phrase file."
               + "\nUsing the default phrase.");
         
         phrase = DEFAULT_PHRASE;
      }
      
      return phrase;
   }
   
   public JSecretPhrase(KeyboardLayout layout)
   {
      final int BUTTON_SIZE = 50;
      final int BUTTON_MARGIN = 5;
      final int OFFSET_FACTOR = 20;
      
      this.layout = layout;
      
      setTitle("JSecretPhrase");
      setResizable(false);
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      getContentPane().setLayout(null);
      setSize(600, 350);
      headerSize = -1;
      
      //-----Creates the UI-----//
      
      //Calculates the max width of the first row
      int rowCount = layout.BREAKS[0];
      int totalWidth = (rowCount * BUTTON_SIZE) + (BUTTON_MARGIN * rowCount - 1);
      
      //Calculates the max width of the second row
      rowCount = layout.BREAKS[1] - layout.BREAKS[0];
      totalWidth = Math.max(totalWidth, (layout.OFFSETS[0] * OFFSET_FACTOR) + 
            (rowCount * BUTTON_SIZE) + (BUTTON_MARGIN * rowCount - 1));
      
      //Calculates the max width of the third row
      rowCount = 26 - layout.BREAKS[1];
      totalWidth = Math.max(totalWidth, (layout.OFFSETS[1] * OFFSET_FACTOR) + 
            (rowCount * BUTTON_SIZE) + (BUTTON_MARGIN * rowCount - 1));
      
      int startX = getSize().width / 2 - totalWidth / 2;
      int startY = getSize().height - (3 * BUTTON_SIZE) - (2 * BUTTON_MARGIN) - 32;
      int xOffset = 0;
      int yOffset = 0;
      
      letters = new JButton[26];
      int breakInd = 0;
      
      for (int i = 0; i < letters.length; i++)
      {
         if (breakInd < layout.BREAKS.length && i == layout.BREAKS[breakInd])
         {
            yOffset += BUTTON_SIZE + BUTTON_MARGIN;
            xOffset = layout.OFFSETS[breakInd] * OFFSET_FACTOR;
            breakInd++;
         }
         
         JButton button = new JButton();
         
         button.addActionListener((e) -> letterPress(button));
         button.setBounds(startX + xOffset, startY + yOffset, BUTTON_SIZE, BUTTON_SIZE);
         
         this.getContentPane().add(button);
         
         letters[i] = button;
         xOffset += BUTTON_SIZE + BUTTON_MARGIN;
      }
      
      JPanel pnl_output = new JPanel();
      pnl_output.setBounds(0, 119, 600, 39);
      
      lbl_output = new JLabel();
      lbl_output.setFont(new Font("Tahoma", Font.PLAIN, 24));
      
      pnl_output.add(lbl_output);
      getContentPane().add(pnl_output);
      
      initializeGame();
   }
   
   //Checks how many times a letter appears in a phrase and adds it to the display
   public int checkLetter(char letter)
   {
      letter = Character.toLowerCase(letter);
      int start = 0;
      int count = 0;
      
      while ((start = phrase.indexOf(letter, start)) > -1)
      {
         outputText.setCharAt(start, letter);
         ++start;
         ++count;
      }
      
      return count;
   }
   
   //Sets the phrase to a random phrase and resets all the game variables
   public void initializeGame()
   {
      phrase = loadPhrase();
      outputText = new StringBuilder(phrase);
      
      letterCount = 0;
      mistakes = 0;
      correct = 0;
      
      //Counts the number of letters and hides them in the display string
      for (int i = 0, length = outputText.length(); i < length; i++)
      {
         if (Character.isLetter(outputText.charAt(i)))
         {
            letterCount++;
            outputText.setCharAt(i, PLACEHOLDER);
         }
      }
      
      //Resets all the buttons' texts to their letter values
      for (int i = 0; i < letters.length; i++)
         letters[i].setText(layout.KEY_ORDER.charAt(i) + "");
      
      updateOutput();
      
      manColor = COLORS[(int)(Math.random() * COLORS.length)];
      
      if (isVisible())
         repaint();
   }
   
   //Runs whenever the user presses a button
   private void letterPress(JButton button)
   {
      char buttonChar = button.getText().charAt(0);
      if (buttonChar == '*')
         return;
      
      button.setText('*' + "");
      
      int hits = checkLetter(buttonChar);
      
      if (hits > 0)
      {
         updateOutput();
         correct += hits;
         
         if (correct == letterCount)
         {
            int result = JOptionPane.showConfirmDialog(this, 
                  END_TEXT[mistakes] + "\nWould you like to play again?", 
                  "You win!", JOptionPane.YES_NO_OPTION);
            
            if (result == JOptionPane.YES_OPTION)
               initializeGame();
            else
               this.dispose();
         }
      }
      else
      {
         mistakes++;
         repaint();
         
         if (mistakes == MAX_MISTAKES)
         {
            outputText.delete(0, outputText.length());
            outputText.append(phrase);
            updateOutput();
            
            int result = JOptionPane.showConfirmDialog(this, 
                  END_TEXT[mistakes] + "\n"
                  + "The phrase was: \"" + phrase + "\"\n"
                  + "Would you like to play again?", 
                  "You lost!", JOptionPane.YES_NO_OPTION);
            
            if (result == JOptionPane.YES_OPTION)
               initializeGame();
            else
               this.dispose();
         }
      }
   }
   
   //Draws the Hangman to the screen
   @Override
   public void paint(Graphics g)
   {
      super.paint(g);
      
      final int[] GALLOWS_LINES = 
      { 
         20, 20, 20, 80, //Main body
         10, 80, 20, 70, //Left leg
         30, 80, 20, 70, //Right leg
         20, 20, 50, 20, //Overhang
         50, 20, 50, 25, //Noose
         20, 30, 30, 20  //Overhang support
      };
      final int DRAWING_SIZE = 80;
      final int Y_OFFSET = 16;
      final int HEAD_SIZE = 10;
      final int BODY_SIZE = 20;
      final int X_OFFSET = (getSize().width / 2) - DRAWING_SIZE / 2;
      final int BODY_X_OFFSET = X_OFFSET + (int)(HEAD_SIZE * 1.5f);
      final int BODY_TOP_Y = Y_OFFSET + GALLOWS_LINES[23] + (int)(HEAD_SIZE * 1.5f);
      final int BODY_BOTTOM_Y = BODY_TOP_Y + BODY_SIZE;
      final int BODY_X = BODY_X_OFFSET + GALLOWS_LINES[22] + HEAD_SIZE / 2;
      final int LIMB_X_OFF = 5;
      final int LIMB_Y_OFF = 18;
      
      //Draws Gallows
      g.setColor(Color.BLACK);
      for (int i = 0; i < GALLOWS_LINES.length; i += 4) 
      {
         g.drawLine(X_OFFSET + GALLOWS_LINES[i], Y_OFFSET + GALLOWS_LINES[i + 1], 
               X_OFFSET + GALLOWS_LINES[i + 2], Y_OFFSET + GALLOWS_LINES[i + 3]);
      }
      
      //Draws Hangman
      g.setColor(manColor);
      switch (mistakes)
      {
         case 6: //Right leg
            g.drawLine(BODY_X, BODY_BOTTOM_Y, BODY_X + LIMB_X_OFF, 
                  BODY_BOTTOM_Y + LIMB_Y_OFF);
         case 5: //Left leg
            g.drawLine(BODY_X, BODY_BOTTOM_Y, BODY_X - LIMB_X_OFF, 
                  BODY_BOTTOM_Y + LIMB_Y_OFF);
         case 4: //Right arm
            g.drawLine(BODY_X, BODY_TOP_Y, BODY_X + LIMB_X_OFF, 
                  BODY_TOP_Y + LIMB_Y_OFF);
         case 3: //Left arm
            g.drawLine(BODY_X, BODY_TOP_Y, BODY_X - LIMB_X_OFF, 
                  BODY_TOP_Y + LIMB_Y_OFF);
         case 2: //Body
            g.drawLine(BODY_X, BODY_TOP_Y, BODY_X, BODY_BOTTOM_Y);
         case 1: //Head
            g.drawArc(BODY_X_OFFSET + GALLOWS_LINES[22], 
                  Y_OFFSET + GALLOWS_LINES[23] + HEAD_SIZE / 2, 
                  HEAD_SIZE, HEAD_SIZE, 0, 360);
      }
   }

   //Sets lbl_output's text to the outputText string
   //  Used to use HTML formatting, but it wouldn't work for some reason.
   //  Maybe test with another layout later
   public void updateOutput()
   {
      lbl_output.setText(outputText.toString());
   }
   
   public enum KeyboardLayout
   {
      ABC   ("ABCDEFGHIJKLMNOPQRSTUVWXYZ", new int[] {9, 18},  new int[] {0, 1}),
      QWERTY("QWERTYUIOPASDFGHJKLZXCVBNM", new int[] {10, 19}, new int[] {1, 2}),
      AZERTY("AZERTYUIOPQSDFGHJKLMWXCVBN", new int[] {10, 20}, new int[] {1, 2});
      
      public final String KEY_ORDER;
      public final int[] BREAKS;
      public final int[] OFFSETS;
      
      private KeyboardLayout(String keyOrder, int[] breaks, int[] offsets)
      {
         KEY_ORDER = keyOrder;
         BREAKS = breaks;
         OFFSETS = offsets;
      }
   }
}

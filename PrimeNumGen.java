// Not seeing a noticeable time increase. Time display when calculating prime numbers isn't displaying correctly.
// Original time for 250,000 - 6.352
// Original time for 500, 000 - 23.565

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class PrimeNumGen extends JFrame
{

    private final JTextArea aTextField = new JTextArea();
    private final JButton primeButton = new JButton("Start");
    private final JButton cancelButton = new JButton("Cancel");
    private volatile boolean cancel = false;
    private final PrimeNumGen thisFrame;
    private AtomicInteger totalPrimeNumbers = new AtomicInteger(0);
    private static List<Integer> primeList = Collections.synchronizedList(new ArrayList<>());
    private int numberOfThreads = Runtime.getRuntime().availableProcessors();



    public static void main(String[] args)
    {
        PrimeNumGen png = new PrimeNumGen("Primer Number Generator");

        // don't add the action listener from the constructor
        png.addActionListeners();
        png.setVisible(true);

        System.out.println("Total Processors: " + Runtime.getRuntime().availableProcessors() );

    }

    private PrimeNumGen(String title)
    {
        super(title);
        this.thisFrame = this;
        cancelButton.setEnabled(false);
        aTextField.setEditable(false);
        setSize(400, 200);
        setLocationRelativeTo(null);
        //kill java VM on exit
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(primeButton,  BorderLayout.SOUTH);
        getContentPane().add(cancelButton,  BorderLayout.EAST);
        getContentPane().add( new JScrollPane(aTextField),  BorderLayout.CENTER);
    }

    private class CancelOption implements ActionListener
    {
        public void actionPerformed(ActionEvent arg0)
        {
            cancel = true;
        }
    }

    private void addActionListeners()
    {
        cancelButton.addActionListener(new CancelOption());

        primeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {

                String num = JOptionPane.showInputDialog("Enter a large integer");
                Integer max =null;

                try
                {
                    max = Integer.parseInt(num);
                }
                catch(Exception ex)
                {
                    JOptionPane.showMessageDialog(
                            thisFrame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }

                if( max != null)
                {
                    aTextField.setText("");
                    primeButton.setEnabled(false);
                    cancelButton.setEnabled(true);
                    cancel = false;
                    new Thread(new UserInput(max)).start();

                }
            }});
    }

    private boolean isPrime( int i)
    {
        for( int x=2; x < i -1; x++)
            if( i % x == 0  )
                return false;

        return true;
    }

    // Prime threads that will run through a given number to find all primes within it's bounds
    private class primeThread implements Runnable
    {
        private int start;
        private int max;
        private Semaphore sem;

        private primeThread(int start, int max, Semaphore sem)
        {
            this.start = start;
            this.max = max;
            this.sem = sem;
        }

        public void run()
        {
            try
            {
                sem.acquire();
            }
            catch(InterruptedException e)
            {
                System.out.println("Could not acquire lock");
                e.printStackTrace();
            }

            for(int i=start; i < max && !cancel; i++)
            {
                if(isPrime(i))
                {
                    primeList.add(i);
                    totalPrimeNumbers.getAndIncrement();
                }
            }
            sem.release();
        }
    }

   private class primeUpdate implements Runnable
    {
        private int max;
        private Semaphore sem;
        private long startTime = System.currentTimeMillis();

        private primeUpdate(int max, Semaphore sem)
        {
            this.max = max;
            this.sem = sem;
        }

        public void run()
        {
            //Shows how many prime numbers have been found in X amount of time
            long lastUpdate = System.currentTimeMillis();
            while(sem.availablePermits() != numberOfThreads)
                {
                    if (System.currentTimeMillis() - lastUpdate > 500)
                    {
                        final String outString = "Found " + primeList.size() + "of " + max + " in "
                                + ((System.currentTimeMillis() - lastUpdate) / 1000) + " seconds ";

                        SwingUtilities.invokeLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                aTextField.setText(outString);
                            }
                        });

                        aTextField.setText(outString);
                        lastUpdate = System.currentTimeMillis();
                    }
                }
                //Display prime list and the total time it took.
                final StringBuffer buff = new StringBuffer();

                for( Integer i2 : primeList)
                    buff.append(i2 + "\n");

                if( cancel)
                    buff.append("cancelled\n");

                float time = (System.currentTimeMillis() - startTime )/1000f;
                buff.append("Time = " + time + " seconds " );

                SwingUtilities.invokeLater( new Runnable()
                {
                    @Override
                    public void run()
                    {

                        cancel = false;
                        primeButton.setEnabled(true);
                        cancelButton.setEnabled(false);
                        aTextField.setText( (cancel ? "cancelled " : "") +  buff.toString());

                    }
                });

            }
        }

    // Gets user input for max number and starts off the threads based on partitions
    private class UserInput implements Runnable
    {
        private final int max;

        private UserInput(int num)
        {
            this.max = num;
        }

        public void run()
        {
            cancel = false;
            Semaphore sem = new Semaphore(numberOfThreads);
            List<Integer> lowerBounds = new ArrayList<>();
            lowerBounds.add(0);
            lowerBounds.add(1);
            for(int j =numberOfThreads; j > 0; j--)
            {
                lowerBounds.add(max/j);
            }
            for(int i=1; i <= numberOfThreads; i++)
            {
                Thread te = new Thread(new primeThread(lowerBounds.get(i), (max/numberOfThreads)*i, sem));
                te.start();

                System.out.println("Start: " + (lowerBounds.get(i)) + " Stop: " + (max/numberOfThreads)*i);
            }

            Thread t = new Thread(new primeUpdate(max, sem));
            t.start();

        }

    }  // end User Input
}
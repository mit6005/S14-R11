package grep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** Search web pages for lines containing a word. */
public class Grep {
    public static void main(String[] args) throws Exception {
        
        // substring to search for
        String substring = "6.005";
        
        // URLs to search
        String[] urls = new String[] {
                "http://web.mit.edu/6.005/www/sp14/psets/ps0/",
                "http://web.mit.edu/6.005/www/sp14/psets/ps1/",
                "http://web.mit.edu/6.005/www/sp14/psets/ps2/",
        };
        
        // list for accumulating matching lines
        List<Line> matches = Collections.synchronizedList(new ArrayList<Line>());
        
        // queue for sending lines from producers to consumers
        BlockingQueue<Line> queue = new LinkedBlockingQueue<Line>();
        
        Thread[] producers = new Thread[urls.length]; // one producer per URL
        Thread[] consumers = new Thread[2]; // multiple consumers
        
        for (int ii = 0; ii < consumers.length; ii++) { // start Consumers
            Thread consumer = consumers[ii] = new Thread(new Consumer(substring, queue, matches));
            consumer.start();
        }
        
        for (int ii = 0; ii < urls.length; ii++) { // start Producers
            Thread producer = producers[ii] = new Thread(new Producer(urls[ii], queue));
            producer.start();
        }
        
        for (Thread producer : producers) { // wait for Producers to stop
            producer.join();
        }
        
        for (Thread consumer : consumers) { // stop all the consumers
            queue.add(new Stop());
        }
        
        for (Thread consumer : consumers) { // wait for Consumers to stop
            consumer.join();
        }
        
        for (Line match : matches) {
            System.out.println(match);
        }
        System.out.println(matches.size() + " lines matched");
    }
}

class Producer implements Runnable {
    
    private final String url;
    private final BlockingQueue<Line> queue;
    
    Producer(String url, BlockingQueue<Line> queue) {
        this.url = url;
        this.queue = queue;
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
            int lineNumber = 0;
            String line;
            while ((line = in.readLine()) != null) {
                queue.add(new Text(url, lineNumber++, line));
                //Thread.yield(); // DEMO
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
    
}

class Consumer implements Runnable {
    
    private final String pattern;
    private final BlockingQueue<Line> queue;
    private final List<Line> matches;
    
    Consumer(String pattern, BlockingQueue<Line> queue, List<Line> matches) {
        this.pattern = pattern;
        this.queue = queue;
        this.matches = matches;
    }

    public void run() {
        try {
            while (true) {
                Line line = queue.take();
                if (line.isEnd()) {
                    break;
                }
                if (line.text().contains(pattern)) {
                    matches.add(line);
                }
                //Thread.yield(); // DEMO
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }
    
}



interface Line {
    /** @return the filename. Requires !isEnd. */
    public String filename();
    /** @return the line number. Requires !isEnd.  */
    public int lineNumber();
    /** @return the text on the line. Requires !isEnd.  */
    public String text();
    /** @return true if there's no more work to do */
    public boolean isEnd();
}

class Text implements Line {
    private final String filename;
    private final int lineNumber;
    private final String text;
    
    public Text(String filename, int lineNumber, String text) {
        this.filename = filename;
        this.lineNumber = lineNumber;
        this.text = text;
    }
    
    public String filename() {
        return filename;
    }
    
    public int lineNumber() {
        return lineNumber;
    }
    
    public String text() {
        return text;
    }
    
    public boolean isEnd() {
        return false;
    }
    
    @Override public String toString() {
        return filename + ":" + lineNumber + ":" + text;
    }
}

class Stop implements Line {
    
    public String filename() {
        throw new UnsupportedOperationException();
    }
    
    public int lineNumber() {
        throw new UnsupportedOperationException();
    }
    
    public String text() {
        throw new UnsupportedOperationException();
    }
    
    public boolean isEnd() {
        return true;
    }
    
    @Override public String toString() {
        return "STOP";
    }
    
}

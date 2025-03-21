import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;



public class VideoFeedViewer {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Face Recognition Video Feed");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLayout(new BorderLayout());

            JLabel label = new JLabel("Loading video feed...", SwingConstants.CENTER);
            label.setFont(new Font("Serif", Font.BOLD, 18));
            frame.add(label, BorderLayout.NORTH);

            JLabel videoLabel = new JLabel();
            videoLabel.setHorizontalAlignment(SwingConstants.CENTER);
            frame.add(videoLabel, BorderLayout.CENTER);

            new Thread(() -> displayVideo(videoLabel)).start();

            frame.setVisible(true);
        });
    }

    private static void displayVideo(JLabel videoLabel) {
        String url = "http://localhost:5000/video_feed";
        
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            InputStream inputStream = connection.getInputStream();

            // Prepare to read each frame from the MJPEG stream
            byte[] boundaryMarker = "\r\n--frame\r\n".getBytes();  // Standard boundary
            ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream();

            int prev = -1;
            int curr;
            
            while ((curr = inputStream.read()) != -1) {
                // Look for boundary marker sequence
                if (prev == boundaryMarker[0] && curr == boundaryMarker[1]) {
                    byte[] frameBytes = frameBuffer.toByteArray();
                    
                    // Attempt to decode the frame bytes as an image
                    if (frameBytes.length > 0) {
                        BufferedImage image = ImageIO.read(new ByteArrayInputStream(frameBytes));
                        if (image != null) {
                            ImageIcon icon = new ImageIcon(image.getScaledInstance(800, 600, Image.SCALE_SMOOTH));
                            videoLabel.setIcon(icon);
                        } else {
                            System.out.println("Could not convert bytes to image");
                        }
                    }
                    // Clear the buffer after processing
                    frameBuffer.reset();
                } else {
                    frameBuffer.write(curr);
                }
                prev = curr;
            }

        } catch (Exception e) {
            videoLabel.setText("Error loading video feed. Check connection to server.");
            e.printStackTrace();
        }
    }
}

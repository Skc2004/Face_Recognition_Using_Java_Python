import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class tempCode {

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
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(10000);
            connection.connect();

            InputStream inputStream = connection.getInputStream();
            ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            boolean readingFrame = false;

            while (true) {
                int bytesRead = inputStream.read(buffer);
                if (bytesRead == -1) break;

                for (int i = 0; i < bytesRead; i++) {
                    frameBuffer.write(buffer[i]);

                    if (frameBuffer.size() >= 2) {
                        byte[] frameBytes = frameBuffer.toByteArray();
                        if (isFrameBoundary(frameBytes)) {
                            if (readingFrame) {
                                frameBuffer.reset();
                                frameBuffer.write(buffer[i]); // Start new frame buffer
                            } else {
                                readingFrame = true;
                            }
                            continue;
                        }
                    }

                    if (readingFrame && frameBuffer.size() > 0) {
                        BufferedImage image = ImageIO.read(new ByteArrayInputStream(frameBuffer.toByteArray()));
                        if (image != null) {
                            SwingUtilities.invokeLater(() -> {
                                videoLabel.setIcon(new ImageIcon(image));
                            });
                        }
                        frameBuffer.reset();
                    }
                }
            }
        } catch (Exception e) {
            videoLabel.setText("Error loading video feed. Check connection to server.");
            e.printStackTrace();
        }
    }

    private static boolean isFrameBoundary(byte[] buffer) {
        return buffer.length > 3 && buffer[buffer.length - 2] == (byte) '\r' && buffer[buffer.length - 1] == (byte)'\n';
    }
}
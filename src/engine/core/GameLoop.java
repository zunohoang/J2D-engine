package engine.core;

import engine.graphics.Renderer;
import engine.localdata.RepositoryService;
import engine.physics.CollisionManager;
import engine.physics.PhysicsSystem;
import engine.scenes.SceneManager;

import java.awt.*;
import java.awt.image.BufferStrategy;

public class GameLoop extends Canvas {
    private boolean running = true;

    public GameLoop() {
        addKeyListener(new KeyInput());
        addMouseListener(new MouseInput());
        setFocusable(true);
    }

    public void start() {
        RepositoryService.getInstance();
        createBufferStrategy(3); // Tạo BufferStrategy với 3 buffers

        // Khởi động thread vật lý
//        Thread physicsThread = new Thread(this::physicsUpdate);
//        physicsThread.start();

        // Khởi động thread cập nhật
        Thread updateThread = new Thread(this::updateLoop);
        updateThread.start();

        // Khởi động thread render
        Thread renderThread = new Thread(this::renderLoop);
        renderThread.start();
    }

    public void physicsUpdate() {
        while (running) {
            long startTime = System.nanoTime();

            // Gọi hàm kiểm tra va chạm
            CollisionManager.checkCollisionsV2();

            long elapsedTime = System.nanoTime() - startTime;
            long sleepTime = 20_000_000 - elapsedTime; // 20ms = 20 triệu nano

            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime / 1_000_000, (int)(sleepTime % 1_000_000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // tốt hơn e.printStackTrace()
                }
            } else {
                // Nếu mất nhiều hơn 20ms, nhường CPU cho thread khác
                Thread.yield();
            }
        }
    }



    private void updateLoop() {
        final int TARGET_FPS = 200; // Số khung hình mỗi giây (Frames Per Second)
        final int TARGET_UPS = 50; // Số lần cập nhật vật lý mỗi giây (Updates Per Second)
        final long OPTIMAL_FRAME_TIME = 1_000_000_000 / TARGET_FPS; // ~16.67ms mỗi khung hình
        final long OPTIMAL_UPDATE_TIME = 1_000_000_000 / TARGET_UPS; // ~20ms mỗi lần cập nhật vật lý

        long lastFrameTime = System.nanoTime();
        long lastUpdateTime = System.nanoTime();
        double deltaTime = 0;


        while (running) {
            long currentTime = System.nanoTime();

            // Tính thời gian đã trôi qua kể từ lần cập nhật cuối cùng
            long elapsedUpdateTime = currentTime - lastUpdateTime;
            lastUpdateTime = currentTime;
            deltaTime += (double) elapsedUpdateTime / OPTIMAL_UPDATE_TIME;

            // Xử lý FixedUpdate với khoảng thời gian cố định
            while (deltaTime >= 1) {
                CollisionManager.checkCollisionsV2(); // Kiểm tra va chạm
//                PhysicsSystem.update(1.0f / TARGET_UPS); // Cập nhật vật lý
                SceneManager.fixedUpdate(1.0f / TARGET_UPS); // Cập nhật vật lý
                deltaTime--;
            }

            // Xử lý Update và LateUpdate
            SceneManager.update((float) elapsedUpdateTime / 1_000_000_000); // Cập nhật logic game
            SceneManager.lateUpdate((float) elapsedUpdateTime / 1_000_000_000); // Cập nhật cuối khung hình

            // Điều chỉnh thời gian để đảm bảo FPS ổn định
            long frameTime = System.nanoTime() - currentTime;
            long sleepTime = (OPTIMAL_FRAME_TIME - frameTime) / 1_000_000; // Chuyển đổi sang ms

            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime); // Ngủ để tiết kiệm CPU
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void renderLoop() {
        final int TARGET_FPS = 200;
        final long OPTIMAL_TIME = 1_000_000_000 / TARGET_FPS; // ~16.67ms mỗi frame

        long lastLoopTime = System.nanoTime();
        double delta = 0;

        // Tạo BufferStrategy để vẽ
        BufferStrategy bufferStrategy = getBufferStrategy();

        while (running) {
            long now = System.nanoTime();
            long elapsedTime = now - lastLoopTime;
            lastLoopTime = now;

            delta += (double) elapsedTime / OPTIMAL_TIME;

            if (delta >= 1) {
                render(bufferStrategy);
                delta--;
            }

            // Điều chỉnh thời gian để đảm bảo FPS ổn định
            long frameTime = System.nanoTime() - now;
            long sleepTime = (OPTIMAL_TIME - frameTime) / 1_000_000; // Chuyển đổi sang ms

            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime); // Ngủ để tiết kiệm CPU
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void render(BufferStrategy bufferStrategy) {
        // Lấy Graphics từ BufferStrategy
        Graphics g = bufferStrategy.getDrawGraphics();
        Renderer renderer = new Renderer(g);
        try {
            // Xóa màn hình
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());

            // Vẽ các đối tượng game
            SceneManager.draw(renderer);
        } finally {
            // Kết thúc vẽ và hiển thị
            g.dispose();
            bufferStrategy.show();
        }
    }
}
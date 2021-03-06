package rusky.husky;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import rusky.husky.JavLoader;

public class Main extends JPanel implements JavLoader {

	public JavApp game;
	private boolean stopPaint = false;
	private static final long serialVersionUID = 1L;
	private BufferedImage screenBuffer = new BufferedImage(503, 526, BufferedImage.TYPE_INT_ARGB);
	private RepeatedThread paint;
	private RepeatedThread tick;
	private JFrame frame = new JFrame();
	private int frametime = 1000 / 60;
	private int ticktime = 1000 / 60;

	private boolean loaded;

	@Override
	protected void paintComponent(Graphics gr) {
		if (stopPaint || !loaded)
			return;
		try {
			stopPaint = true;
			Graphics g = screenBuffer.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(Color.BLACK);
			game.paint(g);
			gr.drawImage(screenBuffer, 0, 0, null);

			stopPaint = false;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Main(JavApp pGame) {
		game = pGame;
		pGame.setParent(this);

		frame.setResizable(pGame.resizeable());
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setTitle(game.getName());
		screenBuffer = new BufferedImage(pGame.getScreenSize().width, pGame.getScreenSize().height, BufferedImage.TYPE_INT_ARGB);
		screenBuffer.getGraphics().drawString("Loading...", pGame.getScreenSize().width / 2, pGame.getScreenSize().height / 2);
		Toolkit.getDefaultToolkit().setDynamicLayout(false);
		setLayout(null);
		setPreferredSize(pGame.getScreenSize());

		frame.add(this);
		frame.pack();
		frame.setVisible(true);
		frame.createBufferStrategy(2);
		pGame.setFrame(getBounds());
	}

	private void initListeners() {
		frame.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				screenBuffer = new BufferedImage(getSize().width, getSize().height, BufferedImage.TYPE_INT_ARGB);
				game.setFrame(getBounds());
			}
		});
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				game.btnPress(e.getX(), e.getY(), e.getButton());
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				game.btnRelease(e.getX(), e.getY(), e.getButton());
			}
		});
		frame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				if (game.close()) {
					frame.dispose();
					System.exit(0);
				}
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {

			@Override
			public void mouseMoved(MouseEvent e) {
				game.mouseMove(e.getX(), e.getY());
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				game.mouseDrag(e.getX(), e.getY());
			}
		});
		addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				game.keyPress(e.getKeyCode());
			}

			@Override
			public void keyReleased(KeyEvent e) {
				game.keyRelease(e.getKeyCode());
			}

		});
		addMouseWheelListener(event -> game.mouseWheel(event.getPreciseWheelRotation()));
		addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				paint.interval = 1000 / 30;
				tick.interval = 1000 / 30;
			}

			@Override
			public void focusGained(FocusEvent e) {
				paint.interval = frametime;
				tick.interval = ticktime;
			}
		});
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowIconified(WindowEvent e) {
				game.windowStateChanged(WindowState.Minimized);
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				game.windowStateChanged(windowState != WindowState.Minimized ? windowState : WindowState.Normal);
			}
		});
	}

	public static void main(JavApp game) {
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception e1) {
		}

		Main instance = new Main(game);

		instance.paint = new RepeatedThread(instance::repaint, instance.frametime);
		instance.tick = new RepeatedThread(new Runnable() {

			boolean paintScheduled = false;

			@Override
			public void run() {
				if (!instance.isFocusOwner())
					instance.requestFocusInWindow();
				game.tick();
				if (!paintScheduled) {
					instance.paint.run();
					paintScheduled = true;
				}
			}
		}, instance.ticktime);
		game.init();
		instance.initListeners();
		instance.loaded = true;

		instance.tick.run();
	}

	@Override
	public void setFrameTime(int time) {
		frametime = time;
		paint.interval = time;
	}

	@Override
	public void setTickTime(int time) {
		ticktime = time;
		tick.interval = time;
	}

	private WindowState windowState;

	@Override
	public void setWindowState(WindowState state) {
		if (windowState == state)
			return;
		frame.setBounds(100, 100, game.getScreenSize().width, game.getScreenSize().height);
		if (frame.isUndecorated() && state != WindowState.Borderless) {
			frame.dispose();
			frame.setUndecorated(false);
			frame.setVisible(true);
		}
		switch (state) {
		case Borderless:
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			frame.setBounds(0, 0, dim.width, dim.height);
			frame.dispose();
			frame.setUndecorated(true);
			frame.setVisible(true);
			game.setFrame(frame.getBounds());
			break;
		case Maximized:
			frame.setExtendedState(Frame.MAXIMIZED_BOTH);
			break;
		case Minimized:
			frame.setExtendedState(Frame.ICONIFIED);
			break;
		default:
			break;
		}
		if (windowState != WindowState.Minimized)
			windowState = state;
	}
}

package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
//import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.Random;

public class MyGdxGame extends ApplicationAdapter {
	//Textures and Sprites
	private SpriteBatch batch;
	private Texture fundo;
	private Texture canoBaixo;
	private Texture canoTopo;
	private Texture[] passaro;
	private Texture gameOver;

	//Fonts
	private BitmapFont fonte;
	private BitmapFont highScore;

	//Colision Borders
	private Circle passaroCircle;
	private Rectangle canoRectangleTopo;
	private Rectangle canoRectangleBaixo;
	//private ShapeRenderer shape;

	//Game States
	private static boolean inIntro = true;
	private static boolean inGameOver = false;
	private static int inColision = 0;

	//Game Constraints
	//we can't set Gdx.graphics.getWidth() now because Gdx isn't initialized yet
	private static float WIDTH;
	private static float HEIGHT;
	//private static int vx = 2;
	private static int VCANO; //13
	private static int DISTCANO; // 500;
	private static double VELOCITYTOUCH; // 27.0;
	private static double a = 0.6;
	private static double SMOOTH_BIRD = 0.4;
	private static int highestScore;
	private static int maxScore;

	//Shared Preferences
	private static final String SETTING = "com.flappybird.myprefs";
	private static final String HIGHEST_SCORE = "highestscore";
	Preferences prefs;

	//Bird Coordinates and times
	private int y, x, y0;
	private double t, t2, vy, tt; //tt is time in intro state, so that the bird keeps flapping

	//Tube Coordinates
	private int ycanoTopo, ycanoBaixo, xcano;

	//Score
	private int score;

	//Camera and Resolution
	private OrthographicCamera camera;
	private Viewport viewport;
	private final float VIRTUAL_WIDTH = 768;
	private final float VIRTUAL_HEIGHT = 1024;

	//Sound
	Sound sound;

	@Override
	public void create () {
		SetCamera();

		batch = new SpriteBatch();

		SetTextures();
		SetFonts();
		SetConstraints();
		SetInitialState();
		SetColisionBorders();
		//shape = new ShapeRenderer();

		sound = (Sound) Gdx.audio.newSound(Gdx.files.internal("jump.mp3"));


	}

	@Override
	public void render () {
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		if (inIntro){
			Intro();
		}
		else if(inGameOver){
			GameOver();
		}
		else{
			t++;
			//check if t is overflown
			CheckOverflow();

			DrawBackGround();

			ListenTouch();
			SetPassaroPosition();

			GerarCano();

			PrintScore();
			CheckColision();
		}
		//without batch.end(), the emulator crashes because it doesn't know when to finish
		batch.end();

		ColisionSpace();
	}

	public void DrawBackGround(){
		batch.draw(fundo, 0, 0, WIDTH, HEIGHT);
	}

	public void ListenTouch(){
		if(Gdx.input.justTouched()){
			y0 = y;
			t2 = t;
			vy = VELOCITYTOUCH;

			sound.play();
		}
	}

	public void SetPassaroPosition(){
		//x = (x+vx)%WIDTH;
		x = 40;
		//y = Math.max(y0 + (int)(vy*(t-t2)) - (int)(a*(t-t2)*(t-t2)), 0);
		y = (y0 + (int)(vy*(t-t2)) - (int)(a*(t-t2)*(t-t2)));
		batch.draw(passaro[(int)(SMOOTH_BIRD * t) % 3], x, y);
	}

	public void CheckOverflow(){
		if(t>100000){
			t=t-t2;
			t2=0;
		}
	}

	public void GerarCano(){
		if(xcano< -canoTopo.getWidth()){
			Random rand= new Random();
			int posCanoBaixo = rand.nextInt(50)+40;
			ycanoBaixo=(int)(-0.01*posCanoBaixo*canoBaixo.getHeight());
			ycanoTopo= ycanoBaixo + canoBaixo.getHeight() + DISTCANO;
			xcano = (int)WIDTH;
			score++;
		}
		xcano-=VCANO;
		batch.draw(canoBaixo, xcano, ycanoBaixo);
		batch.draw(canoTopo, xcano, ycanoTopo);
	}

	public void Intro(){
		batch.draw(fundo, 0,0, WIDTH, HEIGHT);
		batch.draw(passaro[(int)(SMOOTH_BIRD * (tt++)) % 3], 40, HEIGHT/2);
		batch.draw(canoBaixo, xcano, ycanoBaixo);
		batch.draw(canoTopo, xcano, ycanoTopo);
		highScore.draw(batch, "Max score: "+String.valueOf(maxScore), 40, HEIGHT-40);
		if(Gdx.input.justTouched()) inIntro = false;
	}

	public void PrintScore(){
		fonte.draw(batch, "Score: " + String.valueOf(score), WIDTH - 350, HEIGHT-40);
	}

	public void ColisionSpace(){
		//shape and others geometric shapes, must be outside the batch, because it is rendered by
		//shapeRenderer, not Batch
		passaroCircle.set(x+passaro[0].getWidth()/2, y+passaro[0].getHeight()/2, 30);
		canoRectangleBaixo.set(xcano, ycanoBaixo, canoBaixo.getWidth(), canoBaixo.getHeight());
		canoRectangleTopo.set(xcano, ycanoTopo, canoTopo.getWidth(), canoTopo.getHeight());
		/*	We could also do:
		canoRectangleBaixo = new Rectangle(
			xcano, ycanoBaixo, canoBaixo.getWidth(), canoBaixo.getHeight()
		);*/

		/* To visualize the colision's borders
		shape.begin(ShapeRenderer.ShapeType.Filled);
		shape.circle(passaroCircle.x, passaroCircle.y, passaroCircle.radius);
		shape.rect(canoRectangleBaixo.x, canoRectangleBaixo.y, canoRectangleBaixo.width, canoRectangleBaixo.height);
		shape.rect(canoRectangleTopo.x, canoRectangleTopo.y, canoRectangleTopo.width, canoRectangleTopo.height);
		shape.setColor(Color.RED);
		shape.end();
		*/
	}

	public void CheckColision(){
		if(Intersector.overlaps(passaroCircle, canoRectangleBaixo) || Intersector.overlaps(passaroCircle, canoRectangleTopo)
				|| y<=0 || y>=HEIGHT-passaro[0].getHeight()){
			/*if(inColision++ == 0){
				score-=2;
			}*/
			inGameOver = true;
		}
		else{
			inColision = 0;
		}
	}

	public void GameOver(){
		//without drawing fundo, it would draw multiples textures
		batch.draw(fundo, 0,0, WIDTH, HEIGHT);
		batch.draw(gameOver, (WIDTH - gameOver.getWidth())/2, HEIGHT/2);
		batch.draw(passaro[0], x, y);
		batch.draw(canoTopo, xcano, ycanoTopo);
		batch.draw(canoBaixo, xcano, ycanoBaixo);
		t++;
		y = (y0 + (int)(vy*(t-t2)) - (int)(a*(t-t2)*(t-t2)));
		if(Gdx.input.justTouched()) {
			inGameOver = false;
			maxScore = Math.max(maxScore, score);
			prefs.putInteger(HIGHEST_SCORE, maxScore);
			prefs.flush();
			SetInitialState();
			inIntro = true;
		}


	}
	public void SetConstraints(){
		WIDTH=VIRTUAL_WIDTH;
		HEIGHT= VIRTUAL_HEIGHT;
		y0=(int)HEIGHT/2;
		VELOCITYTOUCH = (int) HEIGHT/35;
		DISTCANO = (int)HEIGHT/3;
		VCANO = (int)WIDTH/50;
	}

	public void SetInitialState(){
		t = 0.0;
		t2 = 0.0;
		vy = 0.0;
		tt=0.0;

		y0=(int)HEIGHT/2;
		x=40;

		xcano = (int)WIDTH;
		ycanoTopo=(int)(HEIGHT-canoTopo.getHeight()/4);
		ycanoBaixo=(int)(-0.6*canoBaixo.getHeight());

		score = 0;
	}

	public void SetCamera(){
		camera = new OrthographicCamera();
		camera.position.set(VIRTUAL_WIDTH/2, VIRTUAL_HEIGHT/2, 0);
		viewport = new StretchViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
	}

	public void SetTextures(){
		passaro = new Texture[3];
		passaro[0]= new Texture("passaro1.png");
		passaro[1]= new Texture("passaro2.png");
		passaro[2]= new Texture("passaro3.png");
		fundo = new Texture("fundo.png");
		canoBaixo = new Texture("cano_baixo_maior.png");
		canoTopo = new Texture("cano_topo_maior.png");
		gameOver = new Texture("game_over.png");
	}

	public void SetFonts(){
		fonte = new BitmapFont();
		fonte.setColor(Color.YELLOW);
		fonte.getData().setScale(5);

		prefs = Gdx.app.getPreferences(SETTING);
		highestScore = prefs.getInteger(HIGHEST_SCORE, 0);
		if(highestScore!=0){
			highestScore = prefs.getInteger(HIGHEST_SCORE);
		} else{
			highestScore = 0;
		}
		maxScore = Math.max(maxScore, highestScore);
		highScore = new BitmapFont();
		highScore.setColor(Color.YELLOW);
		highScore.getData().setScale(5);
	}

	public void SetColisionBorders(){
		passaroCircle = new Circle();
		canoRectangleBaixo = new Rectangle();
		canoRectangleTopo = new Rectangle();
	}

	//this method is called when the app is open and the resolution changes, it atualizes the viewport
	@Override
	public void resize(int width, int height) {
		//super.resize(width, height);
		viewport.update(width, height);
	}

	@Override
	public void dispose() {
		//super.dispose();
		passaro[0].dispose();
		passaro[1].dispose();
		passaro[2].dispose();
		canoBaixo.dispose();
		canoTopo.dispose();
		fundo.dispose();
		sound.dispose();
	}
}

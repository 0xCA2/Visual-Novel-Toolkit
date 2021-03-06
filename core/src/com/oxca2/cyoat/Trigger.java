package com.oxca2.cyoat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.Timer.Task;
import com.oxca2.cyoat.GameChoiceMenu.GameChoice;

// dataID
// TriggerID

/*
 * There is a difference between dataID and triggerID
 * 
 * triggerID would be the ID for that specific trigger.
 * This is used to get triggers out of the triggerMap,
 * map triggers to menu items, etc
 * 
 * dataID is used to identify the data that the trigger is working 
 * with, i.e what will be drawn to the screen in a DrawingComand, etc.
 * Multiple triggers can work with the same data, for 
 * example, multiple triggers can work with the same Sprite/Texture.
 * Specifically, dataIDs are used to specify which command either 
 * already works with the data, or the id of the command which 
 * will work with the data.  
 */
public abstract class Trigger {
	int type;
	
	String dataID; 
	String triggerID; //if it's a menu item, it has a unique ID for itself as a menu item. 
	String name;
	int layer;
	int time;
	int line;
	
	
	SceneScreen scene;
	Main game;
	
	abstract void execute();
	
	public void setScene(SceneScreen scene){
		this.scene = scene;
	}
	
	public void setGame(Main game){
		this.game = game;
	}
}

abstract class UpdateCommand {
	String id;
	abstract void update(float delta);
	abstract void end();
}

class FadeOutMusic extends UpdateCommand {
	MusicCommand music;
	SceneScreen scene;
	float step;

	public FadeOutMusic(String id, SceneScreen scene, float step) {
		this.id = id;
		this.music = (MusicCommand) scene.getAudio(id);
		this.scene = scene;
		this.step = step;
	}
	
	@Override
	void update(float delta) {
		float volume = music.getVolume();
		
		if (volume - step >= 0f){
			music.setVolume((float)(volume - step));
		}else {
			end();
		}
	}

	@Override
	void end() {
		scene.removeAudio(music);
		scene.removeUpdate(this);
	}		
}

class StartMusicFadeOut extends Trigger {
	float step; 
	
	@Override
	void execute() {
		scene.addUpdate(new FadeOutMusic(dataID, scene, step));
	}
}

// need to add triggers for adding and removing regular images

class FadeOutBackground extends UpdateCommand {
	
	SceneScreen scene;
	long fadeMillis;
	float fadeOutTime;
	final int MILLIS_IN_SECONS = 1000;
	float interpCoef = 0f; //interpolation coefficient  
	int layer;
	DrawingCommand drawBG;
	
	public FadeOutBackground(String id, int layer, SceneScreen scene, float time){
		this.id = id;
		this.scene = scene;
		this.layer = layer;
		fadeMillis = TimeUtils.millis();
		fadeOutTime = time * MILLIS_IN_SECONS;
		System.out.println(fadeOutTime);
		drawBG = scene.getDrawingCommand(layer, id);
	}
	
	@Override
	void update(float delta) {
		System.out.println(TimeUtils.millis() - fadeMillis);
		if (TimeUtils.millis() - fadeMillis< fadeOutTime){
			interpCoef = (TimeUtils.millis() - fadeMillis) / fadeOutTime;
			System.out.println("before color: " + drawBG.sprite.getColor());
			drawBG.sprite.setColor(Color.WHITE.cpy().lerp(Color.BLACK, interpCoef));
			System.out.println("after color: " + drawBG.sprite.getColor());
		} else {
			end();
		}
	}

	@Override
	void end() {
		scene.removeCommandWithTexture(layer, id);
		scene.removeUpdate(this);
	}
	
}

class StartFadeOutBackground extends Trigger{
	float fadeTime;
	
	@Override
	void execute() {
		scene.addUpdate(new FadeOutBackground(dataID, layer, scene, fadeTime));
	}
	
}

/* The IDs for the Commands are the dataIDs
 * for the triggers
 */
abstract class DrawingCommand {
	SpriteBatch batch;
	Sprite sprite;
	Texture texture;
	String id;
	int layer;
	
	private int x, y;
	private int width, height;
	
	abstract void draw(SpriteBatch batch);
	
	public void setBounds(int x, int y, int width, int height){
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	abstract void dispose();
}

abstract class AudioCommand {
	String id;
	float volume;
	boolean looping;
	
	abstract void play();
	abstract void stop();
	abstract void setVolume(float volume);
	abstract float getVolume();
	abstract void setLooping(boolean looping);
	abstract void dispose();
	
}

class SoundCommand extends AudioCommand {
	Sound sound;
	long soundID;
	
	public SoundCommand(String path, String id, float volume, boolean looping){
		sound = Gdx.audio.newSound(Gdx.files.internal(path));
		this.id = id;
		this.volume = volume;
		this.looping = looping;
		
	}
	
	public void play() {
		if (!looping){
			soundID = sound.play(volume);
		} else {
			soundID = sound.loop(volume);
		}
	}
	
	public void setVolume(float volume){
		sound.setVolume(soundID, volume);
	}
	
	public void stop() {
		sound.stop();
	}
	
	public void dispose() {
		sound.dispose();
	}

	@Override
	public void setLooping(boolean looping) {
		this.looping = looping;
	}

	@Override
	float getVolume() {
		return volume;
	}
}

class MusicCommand extends AudioCommand{
	Music music;
	
	public MusicCommand(String path, String id,  float volume, boolean looping){
		music = Gdx.audio.newMusic(Gdx.files.internal(path));
		this.id = id;
		this.volume = volume;
		this.looping = looping;
	}

	@Override
	void play() {
		music.play();
	}

	@Override
	void stop() {
		music.stop();
	}

	@Override
	void setVolume(float volume) {
		music.setVolume(volume);
	}
	
	
	@Override
	void setLooping(boolean looping) {
		music.setLooping(looping);
	}

	@Override
	void dispose() {
		music.dispose();
	}

	@Override
	float getVolume() {
		return music.getVolume();
	}
}

class AddSound extends Trigger {
	String path; 
	float volume;
	boolean looping;
	
	@Override
	void execute() {
		scene.addAudio(new SoundCommand(path, dataID, volume, looping));
	}
}

class PlaySound extends Trigger {

	@Override
	void execute() {
		scene.getAudio(dataID).play();
	}
}

class AddMusic extends Trigger {
	String path; 
	float volume;
	boolean looping;
		
	@Override
	void execute() {
		scene.addAudio(new MusicCommand(path, dataID, volume, looping));
	}
		
}

class PlayMusic extends Trigger {

	@Override
	void execute() {
		scene.getAudio(dataID).play();
	}	
}

abstract class MultiTriggerSequence extends Trigger{
	Array<Trigger> triggers = new Array<Trigger>();	
	String[] triggerIDs;
	

	protected void mapIDsToTriggers() {
		ObjectMap<String, Trigger> map = scene.getTriggers();
		
		for (String id  : triggerIDs)
			triggers.add(map.get(id));		
	}
}
	
class StartTimeBasedSequence extends MultiTriggerSequence {
	
	@Override
	void execute() {
		mapIDsToTriggers();
		TimeTriggerHandler.scheduleTriggers(triggers);
	}
	
}

class RunMultipleTriggers extends MultiTriggerSequence {

	
	@Override
	void execute() {
		mapIDsToTriggers();
		for (Trigger trigger: triggers)
			trigger.execute();
	}
		
}


class RunAfterAnimatedTextClicked extends MultiTriggerSequence {
	DrawAnimatedText animCommand;
	
	float delay;
	
	@Override
	void execute() {
		mapIDsToTriggers();
		animCommand = (DrawAnimatedText) scene.getLayers().get(layer).get(dataID);
		animCommand.animText.addObserver(
				new AnimatedTextAfterClickObserver(scene, triggers, delay));
	}
	
	
}

class RunDirectlyAfterAnimatedText extends MultiTriggerSequence {
	DrawAnimatedText animCommand;
	float delay;
	
	@Override
	void execute() {
		mapIDsToTriggers();
		animCommand = (DrawAnimatedText) scene.getLayers().get(layer).get(dataID);
		animCommand.animText.clickable = false;
		animCommand.animText.addObserver(
				new AnimatedTextImmediateObserver(scene, triggers, delay));
	}
	
}

class AddNewBackground extends Trigger {
	String bgPath;
	
	@Override
	void execute() {
		scene.addCommandToLayer(new DrawBackground(layer, dataID, bgPath));
	}
}

class SetBackground extends Trigger {
	String bgPath;
	
	@Override
	void execute() {
		scene.setBackground(layer, dataID, new Texture(Gdx.files.internal(bgPath)));
	}	
}


class RemoveBackground extends Trigger {
	@Override
	void execute() {
		scene.removeBackground(this);
	}
}

class DrawBackground extends DrawingCommand {
	
	public DrawBackground(int layer, String id, String bgPath) {
		this.layer = layer;
		this.id = id;
		texture = new Texture(Gdx.files.internal(bgPath));
		sprite  = new Sprite(texture);
		sprite.setSize(Main.WIDTH, Main.HEIGHT);
	}
	
	public void draw(SpriteBatch batch) {
		//batch.draw(sprite, 0, 0, Main.WIDTH, Main.HEIGHT);
		sprite.draw(batch);
	}

	@Override
	void dispose() {
		texture.dispose();
	}
}

class AddTextbox extends Trigger {
	private String bgPath;
	private int x, y;
	private int width, height;
	
	@Override
	void execute() {
		scene.addCommandToLayer(
			new DrawTextbox(layer, dataID, bgPath,
			x, y, width, height));
	}
}

class SetTextbox extends Trigger {
	private String bgPath;
	private int x, y;
	private int width, height;
	
	@Override
	void execute() {
		scene.setTextbox(
			layer, dataID, new Texture(Gdx.files.internal(bgPath)), 
			x, y, width, height);
	}
	
}

class RemoveTextbox extends Trigger {

	@Override
	void execute() {
		scene.removeTextbox(this);
	}
	
}

class DrawTextbox extends DrawingCommand {
	private int x, y;
	private int width, height;
	
	public DrawTextbox(
		int layer, String id, String bgPath, 
		int x, int y, int width, int height) 
	{
		this.layer = layer;
		this.id = id;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		
		texture = new Texture(Gdx.files.internal(bgPath));
		sprite  = new Sprite(texture);		
	}
	
	@Override
	void draw(SpriteBatch batch) {
		batch.draw(sprite, x, y, width, height);
	}

	@Override
	void dispose() {
		texture.dispose();
	}
}

class AddAnimatedText extends Trigger {
	String[] textArray;
	Trigger[] lineTriggers;
	String font;
	
	int x,  y; 
	int lineLength; 
	int maxLines;
	float speed;

	@Override
	void execute() {
		scene.addCommandToLayer(
				new DrawAnimatedText(layer, dataID, game,
				AnimatedText.join(textArray, " "), lineTriggers,
				font, x, y, lineLength, maxLines, speed));		
	}
}

class RemoveAnimatedText extends Trigger {
	@Override
	void execute() {
		scene.removeAnimatedText(this);
	}
}

class DrawAnimatedText extends DrawingCommand {
	AnimatedText animText;
	LineTriggerObserver observer;
	
	public DrawAnimatedText(
		int layer, String id, Main game,
		String text, Trigger[] lineTriggers,
		String font, int x, int y, int lineLength,
		int maxLine, float speed)
	{
		this.layer = layer;
		this.id = id;
		
		animText = new AnimatedText(
			game, text, font, 
			x, y, lineLength, 
			maxLine, speed			
		);
		
		if (lineTriggers != null){
			observer = new LineTriggerObserver(lineTriggers);
			animText.addObserver(observer);
		}
	}
	
	@Override
	void draw(SpriteBatch batch) {
		animText.draw(batch);
	}
	
	public void clearObservers(){
		animText.deleteObservers();
	}

	@Override
	void dispose() {}
}

class AddStaticText extends Trigger {
	String text;
	int x, y;
	String font;
	
	@Override
	void execute() {
		scene.addCommandToLayer(
			new DrawStaticText(
			game, layer, dataID, text, x, y, font));
	}
	
}

class RemoveStaticText extends Trigger {

	@Override
	void execute() {
		scene.removeStaticText(this);
	}
	
}

class DrawStaticText extends DrawingCommand {
	String text;
	int x, y;
	String font;
	BitmapFont bFont;
	
	public DrawStaticText(
		Main game,  int layer, String id,
		String text, int x, int y, String font)
	{
		this.layer = layer;
		this.id = id;
		
		this.text = text;
		this.x = x;
		this.y = y;
		this.font = font;
		
		bFont = game.fonts.get(font);
	}
	
	
	@Override
	void draw(SpriteBatch batch) {
		// TODO Auto-generated method stub
		bFont.draw(batch, text, x, y);
	}


	@Override
	void dispose() {		
	}
	
}

class AddGameChoiceMenu extends Trigger {
	int space,  menuX,  menuY;
	int itemHeight, itemWidth, paddingV,  paddingH;
	float offset;
	String prompt; 
	int promptSpace;
	String font; 
	String[] itemIDs;
	String[] itemNames;
	
	@Override
	void execute() {
		offset = game.fonts.get(font).getLineHeight();
		
		scene.addCommandToLayer(
				new DrawGameChoiceMenu(layer, dataID, game, space, menuX, menuY,
				    itemHeight, itemWidth,  paddingV, paddingH,
				    offset, prompt, promptSpace, font, itemIDs, itemNames, scene));				
	}
	
	
}

class RemoveGameChoiceMenu extends Trigger {

	@Override
	void execute() {
		scene.removeCommandFromLayer(layer, dataID);
	}
	
}

class DrawGameChoiceMenu extends DrawingCommand {
	GameChoiceMenu menu;
	Array<Trigger> menuItems;
	ObjectMap<String, Trigger> map;
	
	public DrawGameChoiceMenu(int layer, String id, Main main,
			int space, int menuX, int menuY,
			int itemHeight, int itemWidth, 
			int paddingV, int paddingH,
			float offset, String prompt, int promptSpace, 
			String font, String[] itemIDs, 
			String[] itemNames, SceneScreen scene)
	{
		this.layer = layer;
		this.id = id;
		menuItems = new Array<Trigger>();
		map = scene.getTriggers();
		
		for (int i = 0; i < itemIDs.length; i++){
			menuItems.add(map.get(itemIDs[i]));
		}
		
		menu = new GameChoiceMenu(main, space, 
			menuX, menuY, itemHeight, itemWidth,
			paddingV, paddingH, offset, prompt, promptSpace, font);
		
		int counter = 0;
		for (Trigger item: menuItems){
			menu.add(menu.new GameChoice(itemNames[counter], item, scene) );
			counter++;
		}
		
		menu.layoutMenu();
		Gdx.input.setInputProcessor(menu);
	}
	
	@Override
	void draw(SpriteBatch batch) {
		menu.draw(batch);		
	}

	@Override
	void dispose() {}
	
}

class AddMenu extends Trigger {
	int space,  menuX,  menuY;
	int itemHeight, itemWidth, paddingV,  paddingH;
	float offset;
	String font; 
	String[] itemIDs;
	String[] itemNames;
	
	@Override
	void execute() {
		offset = game.fonts.get(font).getLineHeight();
		scene.addCommandToLayer(
				new DrawMenu(layer, dataID, game, space, menuX, menuY,
				    itemHeight, itemWidth,  paddingV, paddingH,
				    offset, font, itemIDs, itemNames, scene));	
	}
		
}

class DrawMenu extends DrawingCommand {
	Menu menu;
	Array<Trigger> menuItems;
	ObjectMap<String, Trigger> map;
	
	public DrawMenu(int layer, String id, Main main,
			int space, int menuX, int menuY,
			int itemHeight, int itemWidth, 
			int paddingV, int paddingH,
			float offset,  String font, String[] itemIDs, 
			String[] itemNames, SceneScreen scene)
	{
		this.layer = layer;
		this.id = id;
		menuItems = new Array<Trigger>();
		map = scene.getTriggers();
		
		menu = new Menu(main, space,
			menuX, menuY,itemHeight, itemWidth,
			paddingV, paddingH, offset);
		
		initMenuItems(itemIDs);
		addItemsToMenu(itemNames, scene);
		
		menu.layoutMenu();
		Gdx.input.setInputProcessor(menu);
	}
	
	
	
	void initMenuItems(String[] itemIDs){
		for (int i = 0; i < itemIDs.length; i++){
			menuItems.add(map.get(itemIDs[i]));
		}	
	}
	
	void addItemsToMenu(String[] itemNames, SceneScreen scene) {
		int count = 0;
		for (Trigger item: menuItems){
			menu.add(menu.new DefaultMenuItem(itemNames[count], item, scene));
			count++;
		}
	}
	
	@Override
	void draw(SpriteBatch batch){
		menu.draw(batch);	
	}

	@Override
	void dispose() {}
}

class RemoveCommandFromLayer extends Trigger {

	@Override
	void execute() {
		scene.removeCommandFromLayer(layer, dataID);		
	}
	
}

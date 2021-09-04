package com.game.javatestapp;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.lang.Integer.parseInt;

public class GameActivity extends AppCompatActivity {
    //hero that player choses
    final Random random = new Random();
    int a2cooldown = 2;
    int specialCooldown = 4;
    //hero or enemy class
    private class Hero {
        public int hp;
        public String attackOne;
        public String attackTwo;
        public String specialAttack;
        public String Name;
        //debuff name and duration
        Map<String, Integer> debuffs = new HashMap<String, Integer>();
        //constructor calls reset function, to reset fight on win or lose
        public Hero(int hitPoints, String a1, String a2, String sa, String n) {
            reset(hitPoints, a1, a2, sa, n);
        }
        public void reset(int hitPoints, String a1, String a2, String sa, String n) {
            hp = hitPoints;
            attackOne = a1;
            attackTwo = a2;
            specialAttack = sa;
            Name = n;
            if (debuffs != null) {
                debuffs.clear();
            }
        }
    }
    //check if player selected hero
    boolean selected = false;
    //turn counter, different abilities have cooldowns
    int turn = 0;
    //Hero class that player chooses
    Hero player;
    //Max for hbars for smoother animations
    private void setProgressMax(ProgressBar pb, int max) {
        pb.setMax(max * 100);
    }
    //hpbars animation
    private void setProgressAnimate(ProgressBar pb, int progressTo)
    {
        ObjectAnimator animation = ObjectAnimator.ofInt(pb, "progress", pb.getProgress(), progressTo * 100);
        animation.setDuration(1000);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }
    //map of attacks and their effects
    Map<String, ArrayList<String>> attacks = new HashMap<String, ArrayList<String>>();

    //debuff handler
    private void debuffTick(Hero h) {
        //if there is debuff, draw it's icon
        ImageView bleed = findViewById(R.id.bleedDebuff);
        ImageView blind = findViewById(R.id.blindDebuff);
        ImageView stun = findViewById(R.id.stunDebuff);
        ImageView fire = findViewById(R.id.onfireDebuff);
        if (h.debuffs.get("Bleed") != null) {
            bleed.setVisibility(View.VISIBLE);
            h.hp -= 10;
            if ((h.debuffs.get("Bleed") - 1) == 0) {
                //if debuff ended, hide it's icon
                h.debuffs.remove("Bleed");
                bleed.setVisibility(View.GONE);
            } else {
                h.debuffs.replace("Bleed", h.debuffs.get("Bleed") - 1);
            }
        } else {
            bleed.setVisibility(View.GONE);
        }
        if (h.debuffs.get("Fire") != null) {
            fire.setVisibility(View.VISIBLE);
            h.hp -= 3;
            if ((h.debuffs.get("Fire") - 1) == 0) {
                fire.setVisibility(View.GONE);
                h.debuffs.remove("Fire");
            } else {
                h.debuffs.replace("Fire", h.debuffs.get("Fire") - 1);
            }
        } else {
            fire.setVisibility(View.GONE);
        }
        if (h.debuffs.get("Stun") != null) {
            stun.setVisibility(View.VISIBLE);
            if ((h.debuffs.get("Stun") - 1) == 0) {
                stun.setVisibility(View.GONE);
                h.debuffs.remove("Stun");
            } else {
                h.debuffs.replace("Stun", h.debuffs.get("Stun") - 1);
            }
        } else {
            stun.setVisibility(View.GONE);
        }
        if (h.debuffs.get("Blind") != null) {
            blind.setVisibility(View.VISIBLE);
            if ((h.debuffs.get("Blind") - 1) == 0) {
                blind.setVisibility(View.GONE);
                h.debuffs.remove("Blind");
            } else {
                h.debuffs.replace("Blind", h.debuffs.get("Blind") - 1);
            }
        } else {
            blind.setVisibility(View.GONE);
        }
    }
    //handle turns
    private void makeTurn(Hero h, Hero e, String heroAction, Map<String, ArrayList<String>> attackTable) {
        //base attack damage
        int heroAttackDmg = parseInt(attackTable.get(heroAction).get(0));
        //debuff, if attack has one
        String heroAttackDebuff = attackTable.get(heroAction).get(1);
        //default value if attack doesn't have debuff
        int heroAttackDebuffDuration = -1;
        //get debuff duration, if attack has debuff
        if (heroAttackDebuff.length() > 0) {
            heroAttackDebuffDuration = parseInt(attackTable.get(heroAction).get(2));
        }
        //rogue can bleed and blind enemy
        if (h.Name.equals("Rogue")) {
            //if attack has debuff
            if (heroAttackDebuffDuration > -1) {
                //if enemy has this debuff prolong it, else apply new debuff
                if (e.debuffs.get(heroAttackDebuff) != null) {
                    e.debuffs.replace(heroAttackDebuff, e.debuffs.get(heroAttackDebuff) + heroAttackDebuffDuration);
                } else {
                    e.debuffs.put(heroAttackDebuff, heroAttackDebuffDuration);
                }
            }
            //handling hero attack
            e.hp -= heroAttackDmg;
            //30% chance for double damage
            if (random.nextInt(101) > 70) {
                e.hp -= heroAttackDmg;
            }
            if (e.debuffs.get("Blind") == null) {
                //50% chance to dodge
                if (random.nextInt(101) > 50) {
                    h.hp -= 15;
                }
                //100% cnance to dodge if enemy is blind
            }
            debuffTick(e);
            //warrior can only stun enemy
        } else if (h.Name.equals("Warrior")) {
            //if attack has debuff
            if (heroAttackDebuffDuration > -1) {
                //if enemy has this debuff prolong it, else apply new debuff
                if (e.debuffs.get(heroAttackDebuff) != null) {
                    e.debuffs.replace(heroAttackDebuff, e.debuffs.get(heroAttackDebuff) + heroAttackDebuffDuration);
                } else {
                    e.debuffs.put(heroAttackDebuff, heroAttackDebuffDuration);
                }
            }
            e.hp -= heroAttackDmg;
            //if enemy isn't stunned
            if (e.debuffs.get("Stun") == null) {
                //Warrior has 25% defense
                h.hp -= Math.round(15*0.75);
            }
            debuffTick(e);
            //mage can set enemy on fire
        } else if (h.Name.equals("Mage")) {
            //if mage attacks
            if (!heroAction.equals("Recharge")) {
                //if attack has debuff
                if (heroAttackDebuffDuration > -1) {
                    //if enemy has this debuff prolong it, else apply new debuff
                    if (e.debuffs.get(heroAttackDebuff) != null) {
                        e.debuffs.replace(heroAttackDebuff, e.debuffs.get(heroAttackDebuff) + heroAttackDebuffDuration);
                    } else {
                        e.debuffs.put(heroAttackDebuff, heroAttackDebuffDuration);
                    }
                }
                e.hp -= heroAttackDmg;
                h.hp -= 15;
                debuffTick(e);
            } else {
                //restore hp to max on recharge
                h.hp = 61;
            }
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //initialize attack table
        attacks.put("Stab", new ArrayList<String>());
        attacks.get("Stab").add("10"); //base dmg
        attacks.get("Stab").add(""); //debuff
        attacks.get("Stab").add(""); //debuff duration
        attacks.put("Dust", new ArrayList<String>());
        attacks.get("Dust").add("0");
        attacks.get("Dust").add("Blind");
        attacks.get("Dust").add("2");
        attacks.put("DoubleBlow", new ArrayList<String>());
        attacks.get("DoubleBlow").add("20");
        attacks.get("DoubleBlow").add("Bleed");
        attacks.get("DoubleBlow").add("2");
        attacks.put("Strike", new ArrayList<String>());
        attacks.get("Strike").add("15");
        attacks.get("Strike").add("");
        attacks.get("Strike").add("");
        attacks.put("ShieldBlow", new ArrayList<String>());
        attacks.get("ShieldBlow").add("5");
        attacks.get("ShieldBlow").add("Stun");
        attacks.get("ShieldBlow").add("2");
        attacks.put("HeavyStrike", new ArrayList<String>());
        attacks.get("HeavyStrike").add("30");
        attacks.get("HeavyStrike").add("");
        attacks.get("HeavyStrike").add("");
        attacks.put("FireBall", new ArrayList<String>());
        attacks.get("FireBall").add("5");
        attacks.get("FireBall").add("Fire");
        attacks.get("FireBall").add("5");
        attacks.put("FrostBolt", new ArrayList<String>());
        attacks.get("FrostBolt").add("5");
        attacks.get("FrostBolt").add("");
        attacks.get("FrostBolt").add("");
        attacks.put("Smack", new ArrayList<String>());
        attacks.get("Smack").add("15");
        attacks.get("Smack").add("");
        attacks.get("Smack").add("");
        attacks.put("Recharge", new ArrayList<String>());
        attacks.get("Recharge").add("0");
        attacks.get("Recharge").add("");
        attacks.get("Recharge").add("");
        //3 classes to choose from
        Hero rogue = new Hero(50, "Stab", "Dust", "DoubleBlow", "Rogue");
        Hero warrior = new Hero(50, "Strike", "ShieldBlow", "HeavyStrike", "Warrior");
        Hero mage = new Hero(61, "FrostBolt", "FireBall", "Recharge", "Mage");
        //enemy
        Hero goblin = new Hero(75, "Smack", "", "", "Goblin");
        //attack sounds
        final MediaPlayer stabSound = MediaPlayer.create(this, R.raw.stab);
        final MediaPlayer bleedSound = MediaPlayer.create(this, R.raw.bleed);
        final MediaPlayer dustSound = MediaPlayer.create(this, R.raw.dust);
        final MediaPlayer strikeSound = MediaPlayer.create(this, R.raw.strike);
        final MediaPlayer shieldSound = MediaPlayer.create(this, R.raw.shield);
        final MediaPlayer heavySound = MediaPlayer.create(this, R.raw.heavy);
        final MediaPlayer frostSound = MediaPlayer.create(this, R.raw.frost);
        final MediaPlayer fireSound = MediaPlayer.create(this, R.raw.fire);
        final MediaPlayer rechargeSound = MediaPlayer.create(this, R.raw.recharge);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        //get variables for different elements
        Button rogButton = findViewById(R.id.butRog);
        Button warButton = findViewById(R.id.butWar);
        Button mageButton = findViewById(R.id.butMage);
        ImageView hero = findViewById(R.id.imgHero);
        TextView heroName = findViewById(R.id.heroName);
        ProgressBar goblinHP = findViewById(R.id.gobHP);
        ProgressBar heroHP = findViewById(R.id.heroHP);
        TextView hint = findViewById(R.id.hint);
        //setup listeners and events, buttons change for hero abilities when player choses hero
        //listeners that show hints for actions
        //show hints on touch for all buttons, depending on hero
        warButton.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (!selected) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        hint.setText("War has 25% defense");
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        hint.setText("");
                    }
                } else {
                    if (player.Name.equals("Rogue")) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            hint.setText("Throw dust into goblin's eyes!\nGoblin has 50% reduced chance to hit for 1 turn");
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            hint.setText("");
                        }
                    } else if (player.Name.equals("Warrior")) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            hint.setText("Hit goblin with a shield for 5 damage!\nGoblin is stunned for 1 turn");
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            hint.setText("");
                        }
                    } else if (player.Name.equals("Mage")) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            hint.setText("Set goblin on fire with firebolt!\nGoblin loses 5 hp and additional 3 hp every turn for 5 turns");
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            hint.setText("");
                        }
                    }
                }
                return false;
            }
        });
        rogButton.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (!selected) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        hint.setText("Rogue has 50% dodge and 30% crit");
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        hint.setText("");
                    }
                } else {
                    if (player.Name.equals("Rogue")) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            hint.setText("Stab goblin for 10 damage");
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            hint.setText("");
                        }
                    } else if (player.Name.equals("Warrior")) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            hint.setText("Strike goblin for 10 damage");
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            hint.setText("");
                        }
                    } else if (player.Name.equals("Mage")) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            hint.setText("Frostbolt goblin for 5 damage");
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            hint.setText("");
                        }
                    }
                }
                return false;
            }
        });
        mageButton.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (!selected) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        hint.setText("Mage can restore full hp with special ability");
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        hint.setText("");
                    }
                } else {
                    if (player.Name.equals("Rogue")) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            hint.setText("Hit goblin with both daggers for 20!\nGoblin bleed for additional 10 damage every turn for 2 turns");
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            hint.setText("");
                        }
                    } else if (player.Name.equals("Warrior")) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            hint.setText("Hit goblin with all your strength for 30");
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            hint.setText("");
                        }
                    } else if (player.Name.equals("Mage")) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            hint.setText("Restore all your hp");
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            hint.setText("");
                        }
                    }
                }
                return false;
            }
        });
        //handle clicks of rogue/attackOne button
        rogButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //if player hasn't selected hero yet, set player to rogue and start the fight
                if (!selected) {
                    selected = true;
                    player = rogue;
                    heroName.setText(player.Name);
                    rogButton.setText(player.attackOne);
                    warButton.setText(player.attackTwo);
                    warButton.setVisibility(View.GONE);
                    mageButton.setText(player.specialAttack);
                    mageButton.setVisibility(View.GONE);
                    hero.setImageResource(R.drawable.rogue);
                    setProgressMax(goblinHP, 75);
                    setProgressMax(heroHP, player.hp);
                    setProgressAnimate(goblinHP, 100);
                    setProgressAnimate(heroHP, 100);
                    turn++;
                } else {
                    //play sounds
                    if (player.Name.equals("Rogue")) {
                        stabSound.start();
                    } else if (player.Name.equals("Warrior")) {
                        strikeSound.start();
                    } else if (player.Name.equals("Mage")) {
                        frostSound.start();
                    }
                    //calculate damage
                    makeTurn(player, goblin, player.attackOne, attacks);
                    turn++;
                    a2cooldown--;
                    specialCooldown--;
                    setProgressAnimate(goblinHP, goblin.hp);
                    setProgressAnimate(heroHP, player.hp);
                }
                //handle abilities with cooldowns
                if (a2cooldown == 0) {
                    warButton.setVisibility(View.VISIBLE);
                }
                if (specialCooldown == 0) {
                    mageButton.setVisibility(View.VISIBLE);
                }
                //handle win or lose
                if (goblin.hp <= 0 && player.hp <=0) {
                    hint.setText("You won!\nBut at what cost :(");
                    rogButton.setVisibility(View.GONE);
                    warButton.setVisibility(View.VISIBLE);
                    warButton.setText("Fight again!");
                    mageButton.setVisibility(View.GONE);
                } else if (goblin.hp <= 0) {
                    hint.setText("You won!");
                    rogButton.setVisibility(View.GONE);
                    warButton.setVisibility(View.VISIBLE);
                    warButton.setText("Fight again!");
                    mageButton.setVisibility(View.GONE);
                } else if (player.hp <= 0) {
                    hint.setText("You lost!");
                    rogButton.setVisibility(View.GONE);
                    warButton.setVisibility(View.VISIBLE);
                    warButton.setText("Fight again!");
                    mageButton.setVisibility(View.GONE);
                }
            }
        });
        //handle clicks of warrior/attackTwo button, same as prev except when fight ends, this button restarts the fight
        warButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!selected) {
                    selected = true;
                    player = warrior;
                    heroName.setText(player.Name);
                    rogButton.setText(player.attackOne);
                    warButton.setText(player.attackTwo);
                    warButton.setVisibility(View.GONE);
                    mageButton.setText(player.specialAttack);
                    mageButton.setVisibility(View.GONE);
                    hero.setImageResource(R.drawable.warrior);
                    setProgressMax(goblinHP, 75);
                    setProgressMax(heroHP, player.hp);
                    setProgressAnimate(goblinHP, 100);
                    setProgressAnimate(heroHP, 100);
                    turn++;
                } else if (!warButton.getText().equals("Fight again!")){
                    if (player.Name.equals("Rogue")) {
                        dustSound.start();
                    } else if (player.Name.equals("Warrior")) {
                        shieldSound.start();
                    } else if (player.Name.equals("Mage")) {
                        fireSound.start();
                    }
                    makeTurn(player, goblin, player.attackTwo, attacks);
                    turn++;
                    a2cooldown = 2;
                    warButton.setVisibility(View.GONE);
                    specialCooldown--;
                    setProgressAnimate(goblinHP, goblin.hp);
                    setProgressAnimate(heroHP, player.hp);
                }
                if (a2cooldown == 0) {
                    warButton.setVisibility(View.VISIBLE);
                }
                if (specialCooldown == 0) {
                    mageButton.setVisibility(View.VISIBLE);
                }
                //handle win or lose
                if (goblin.hp <= 0 && player.hp <=0) {
                    hint.setText("You won!\nBut at what cost :(");
                    rogButton.setVisibility(View.GONE);
                    warButton.setVisibility(View.VISIBLE);
                    warButton.setText("Fight again!");
                    mageButton.setVisibility(View.GONE);
                } else if (goblin.hp <= 0) {
                    hint.setText("You won!");
                    rogButton.setVisibility(View.GONE);
                    warButton.setVisibility(View.VISIBLE);
                    warButton.setText("Fight again!");
                    mageButton.setVisibility(View.GONE);
                } else if (player.hp <= 0) {
                    hint.setText("You lost!");
                    rogButton.setVisibility(View.GONE);
                    warButton.setVisibility(View.VISIBLE);
                    warButton.setText("Fight again!");
                    mageButton.setVisibility(View.GONE);
                }
                //reset fight
                if (warButton.getText().equals("Fight again!")) {
                    selected = false;
                    rogButton.setText("Rogue");
                    rogButton.setVisibility(View.VISIBLE);
                    warButton.setText("Warrior");
                    mageButton.setText("Mage");
                    mageButton.setVisibility(View.VISIBLE);
                    hero.setImageResource(0);
                    setProgressAnimate(goblinHP, 0);
                    setProgressAnimate(heroHP, 0);
                    rogue.reset(50, "Stab", "Dust", "DoubleBlow", "Rogue");
                    warrior.reset(50, "Strike", "ShieldBlow", "HeavyStrike", "Warrior");
                    mage.reset(61, "FrostBolt", "FireBall", "Recharge", "Mage");
                    goblin.reset(75, "Smack", "", "", "Goblin");
                    hint.setText("");
                    heroName.setText("");
                    turn = 0;
                    a2cooldown = 2;
                    specialCooldown = 4;
                    debuffTick(goblin);
                }
            }
        });
        //handle clicks of mage/special ability button
        mageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!selected) {
                    selected = true;
                    player = mage;
                    heroName.setText(player.Name);
                    rogButton.setText(player.attackOne);
                    warButton.setText(player.attackTwo);
                    warButton.setVisibility(View.GONE);
                    mageButton.setText(player.specialAttack);
                    mageButton.setVisibility(View.GONE);
                    hero.setImageResource(R.drawable.mage);
                    setProgressMax(goblinHP, 75);
                    setProgressMax(heroHP, player.hp);
                    setProgressAnimate(goblinHP, 100);
                    setProgressAnimate(heroHP, 100);
                    turn++;
                } else {
                    if (player.Name.equals("Rogue")) {
                        bleedSound.start();
                    } else if (player.Name.equals("Warrior")) {
                        heavySound.start();
                    } else if (player.Name.equals("Mage")) {
                        rechargeSound.start();
                    }
                    makeTurn(player, goblin, player.specialAttack, attacks);
                    turn++;
                    a2cooldown--;
                    specialCooldown = 4;
                    mageButton.setVisibility(View.GONE);
                    setProgressAnimate(goblinHP, goblin.hp);
                    setProgressAnimate(heroHP, player.hp);
                }
                //handle abilities with cooldowns
                if (a2cooldown == 0) {
                    warButton.setVisibility(View.VISIBLE);
                }
                if (specialCooldown == 0) {
                    mageButton.setVisibility(View.VISIBLE);
                }
                //handle win or lose
                if (goblin.hp <= 0 && player.hp <=0) {
                    hint.setText("You won!\nBut at what cost :(");
                    rogButton.setVisibility(View.GONE);
                    warButton.setVisibility(View.VISIBLE);
                    warButton.setText("Fight again!");
                    mageButton.setVisibility(View.GONE);
                } else if (goblin.hp <= 0) {
                    hint.setText("You won!");
                    rogButton.setVisibility(View.GONE);
                    warButton.setVisibility(View.VISIBLE);
                    warButton.setText("Fight again!");
                    mageButton.setVisibility(View.GONE);
                } else if (player.hp <= 0) {
                    hint.setText("You lost!");
                    rogButton.setVisibility(View.GONE);
                    warButton.setVisibility(View.VISIBLE);
                    warButton.setText("Fight again!");
                    mageButton.setVisibility(View.GONE);
                }
            }
        });
    }
}
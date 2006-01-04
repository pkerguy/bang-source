//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Controller;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.VBOInfo;
import com.jme.scene.shape.Sphere;
import com.jme.scene.state.LightState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.TextureState;
import com.jmex.effects.Particle;
import com.jmex.effects.ParticleManager;

import com.threerings.util.RandomUtil;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.Terrain;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays an explosion.
 */
public class ExplosionViz extends ParticleEffectViz
{
    public ExplosionViz (boolean small)
    {
        _small = small;
    }

    @Override // documentation inherited
    public void display (PieceSprite target)
    {
        // set up and add the dust ring
        if (_dustring != null) {
            prepareDustRing(target);
            displayParticleManager(target, _dustring, false);
        }
        
        // add the fireball
        displayParticleManager(target, _fireball, true);
        
        // add the smoke puff
        displayParticleManager(target, _smokepuff, true);
        
        // and the streamers
        for (int i = 0; i < _streamers.length; i++) {
            displayParticleManager(target, _streamers[i].pmgr, true);
        }
        
        // note that the effect was displayed
        effectDisplayed();
    }
    
    @Override // documentation inherited
    protected void didInit ()
    {
        // create the dust ring for explosions on the ground
        if (!_ntarget.isFlyer()) {
            _dustring = ParticleFactory.getDustRing();
        }
        
        // create the fireball
        _fireball = ParticleFactory.getFireball();
        
        // create the smoke puff
        _smokepuff = ParticleFactory.getSmokePuff();
        
        // create a few streamers from the explosion
        _streamers = new Streamer[NUM_STREAMERS_AVG +
            RandomUtil.getInt(+NUM_STREAMERS_DEV, -NUM_STREAMERS_DEV)];
        for (int i = 0; i < _streamers.length; i++) {
            _streamers[i] = new Streamer();
        }
    }
    
    /**
     * (Re)initializes the dust ring particle system for use on the specified
     * kind of terrain.
     */
    protected void prepareDustRing (PieceSprite target)
    {
        Terrain terrain = _view.getBoard().getPredominantTerrain(_ntarget.x,
            _ntarget.y);
        ColorRGBA color = RenderUtil.getGroundColor(terrain);
        _dustring.getStartColor().set(color.r, color.g, color.b,
            terrain.dustiness);
        _dustring.getEndColor().set(color.r, color.g, color.b, 0f);
        
        _dustring.getParticles().setLocalTranslation(
            target.getLocalTranslation());
        _dustring.getParticles().setLocalRotation(
            target.getLocalRotation());
    }
    
    /**
     * Handles a streamer flying from the blast.
     */
    protected class Streamer
    {
        /** The particle manager for the streamer. */
        public ParticleManager pmgr;
        
        public Streamer ()
        {
            pmgr = ParticleFactory.getStreamer();
            pmgr.setActive(true);
            pmgr.setParticlesOrigin(new Vector3f());
            
            // fire the streamer in a random direction
            float azimuth = RandomUtil.getFloat(FastMath.TWO_PI),
                elevation = RandomUtil.getFloat(FastMath.HALF_PI) -
                    FastMath.PI * 0.25f;
            _velocity = new Vector3f(
                FastMath.cos(azimuth) * FastMath.cos(elevation),
                FastMath.sin(azimuth) * FastMath.cos(elevation),
                FastMath.sin(elevation));
            _velocity.mult(TILE_SIZE / 2, pmgr.getParticlesOrigin());
            _velocity.multLocal(STREAMER_INIT_SPEED);
            
            pmgr.getParticles().addController(new Controller() {
                public void update (float time) {
                    // update the position and velocity of the emitter
                    Vector3f origin = pmgr.getParticlesOrigin();
                    origin.scaleAdd(time, _velocity, origin);
                    _velocity.scaleAdd(time, STREAMER_ACCEL, _velocity);
                    
                    // remove streamer if its lifespan has elapsed
                    if ((_age += time) > STREAMER_LIFESPAN) {
                        pmgr.setActive(false);
                        pmgr.getParticles().removeController(this);
                    }
                }
            });
        }
        
        /** The velocity of the streamer's emitter. */
        protected Vector3f _velocity;
        
        /** The age of this streamer in seconds. */
        protected float _age;
    }
    
    protected boolean _small;
    protected ParticleManager _dustring, _fireball, _smokepuff;
    protected Streamer[] _streamers;
    
    protected static TextureState _firetex;
    
    /** The average number of streamers to throw from the explosion. */
    protected static final int NUM_STREAMERS_AVG = 4;
    
    /** The deviation of the number of streamers. */
    protected static final int NUM_STREAMERS_DEV = 2;
    
    /** The initial speed of the streamers. */
    protected static final float STREAMER_INIT_SPEED = 25f;
    
    /** The acceleration of the streamers. */
    protected static final Vector3f STREAMER_ACCEL = new Vector3f(0f, 0f, -100f);
    
    /** The amount of time in seconds to keep the streamers alive. */
    protected static final float STREAMER_LIFESPAN = 5f;
}

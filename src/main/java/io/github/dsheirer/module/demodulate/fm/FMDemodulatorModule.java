/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.module.demodulate.fm;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.Window;
import io.github.dsheirer.dsp.filter.fir.complex.ComplexFIRFilter_CB_CB;
import io.github.dsheirer.dsp.fm.FMDemodulator_CB;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexBuffer;
import io.github.dsheirer.sample.complex.reusable.IReusableComplexBufferListener;
import io.github.dsheirer.sample.complex.reusable.ReusableComplexBuffer;
import io.github.dsheirer.sample.real.IUnFilteredRealBufferProvider;
import io.github.dsheirer.sample.real.RealBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FMDemodulatorModule extends Module implements IReusableComplexBufferListener, IUnFilteredRealBufferProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(FMDemodulatorModule.class);

    private static final int SAMPLE_RATE = 48000;

    private ComplexFIRFilter_CB_CB mIQFilter;
    private FMDemodulator_CB mDemodulator;
    private ReusableBufferListener mReusableBufferListener = new ReusableBufferListener();

    /**
     * FM Demodulator with I/Q filter.  Demodulated output is unfiltered and
     * may contain a DC component.
     *
     * @param pass - pass frequency for IQ filtering prior to demodulation.  This
     * frequency should be half of the signal bandwidth since the filter will
     * be applied against each of the inphase and quadrature signals and the
     * combined pass bandwidth will be twice this value.
     * @param stop - stop frequency for IQ filtering prior to demodulation.
     */
    public FMDemodulatorModule(int pass, int stop)
    {
        assert (stop > pass);

        mIQFilter = new ComplexFIRFilter_CB_CB(FilterFactory.getLowPass(
            SAMPLE_RATE, pass, stop, 60, Window.WindowType.HAMMING, true), 1.0f);

        mDemodulator = new FMDemodulator_CB(1.0f);
        mIQFilter.setListener(mDemodulator);
    }

    public FMDemodulatorModule(float[] filter)
    {
        mIQFilter = new ComplexFIRFilter_CB_CB(filter, 1.0f);
        mDemodulator = new FMDemodulator_CB(1.0f);
        mIQFilter.setListener(mDemodulator);
    }

    @Override
    public Listener<ReusableComplexBuffer> getReusableComplexBufferListener()
    {
        return mReusableBufferListener;
    }

    @Override
    public void dispose()
    {
        mIQFilter.dispose();
        mIQFilter = null;

        mDemodulator.dispose();
        mDemodulator = null;
    }

    @Override
    public void reset()
    {
        mDemodulator.reset();
    }

    @Override
    public void setUnFilteredRealBufferListener(Listener<RealBuffer> listener)
    {
        mDemodulator.setListener(listener);
    }

    @Override
    public void removeUnFilteredRealBufferListener()
    {
        mDemodulator.removeListener();
    }

    @Override
    public void start()
    {
    }

    @Override
    public void stop()
    {
    }

    public class ReusableBufferListener implements Listener<ReusableComplexBuffer>
    {
        @Override
        public void receive(ReusableComplexBuffer reusableComplexBuffer)
        {
            float[] samples = reusableComplexBuffer.getCopyOfSamples();

            //TODO: redesign the filter chain so that we can simply pass a float array ...

            mIQFilter.receive(new ComplexBuffer(samples));
            reusableComplexBuffer.decrementUserCount();
        }
    }
}

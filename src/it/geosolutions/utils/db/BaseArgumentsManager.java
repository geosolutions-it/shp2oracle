/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package it.geosolutions.utils.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.commons.cli2.option.GroupImpl;
import org.apache.commons.cli2.util.HelpFormatter;

/**
 * @author Ivano Picco
 * 
 */
public abstract class BaseArgumentsManager {
	private final static Logger LOGGER = org.geotools.util.logging.Logging
			.getLogger(BaseArgumentsManager.class.toString());

	/**
	 * Options for the command line.
	 */
	private final List<Option> cmdOpts = Collections
			.synchronizedList(new ArrayList<Option>());

	private final Parser cmdParser = new Parser();

	protected final ArgumentBuilder argumentBuilder = new ArgumentBuilder();

	protected final DefaultOptionBuilder optionBuilder = new DefaultOptionBuilder();

	private Group optionsGroup;

	private CommandLine cmdLine;

	private final Option helpOpt;

	private final Option versionOpt;

	private String toolName;

	private String version;

	/**
	 * Default imageio caching behaviour.
	 */
	public final boolean DEFAULT_IMAGEIO_CACHING_BEHAVIOUR = false;

	/** ImageIO caching behvaiour controller. */
	private boolean useImageIOCache = DEFAULT_IMAGEIO_CACHING_BEHAVIOUR;

	/**
	 * 
	 */
	public BaseArgumentsManager(final String name, final String version) {
		super();
		toolName = name;
		this.version = version;
		versionOpt = optionBuilder.withShortName("v")
				.withLongName("version").withDescription(
						"print the version.").create();

		helpOpt = optionBuilder.withShortName("h").withShortName("?")
				.withLongName("help").withDescription("print this message.")
				.create();

		cmdOpts.add(versionOpt);
		cmdOpts.add(helpOpt);
	}

	protected void addOption(Option opt) {
		if (cmdLine != null)
			throw new IllegalStateException();
		synchronized (cmdOpts) {
			cmdOpts.add(opt);
		}

	}

	protected boolean removeOption(Option opt) {
		if (cmdLine != null)
			throw new IllegalStateException();
		synchronized (cmdOpts) {
			return cmdOpts.remove(opt);
		}
	}

	protected boolean removeOptions(List<Option> opts) {
		if (cmdLine != null)
			throw new IllegalStateException();
		synchronized (cmdOpts) {
			return cmdOpts.remove(opts);
		}
	}

	protected void addOptions(List<Option> opts) {
		if (cmdLine != null)
			throw new IllegalStateException();
		synchronized (cmdOpts) {
			cmdOpts.addAll(opts);
		}

	}

	protected void finishInitialization() {
		// /////////////////////////////////////////////////////////////////////
		//
		// Help Formatter
		//
		// /////////////////////////////////////////////////////////////////////
		final HelpFormatter cmdHlp = new HelpFormatter("| ", "  ", " |", 75);
		cmdHlp.setShellCommand(getToolName());
		cmdHlp.setHeader("Help");
		cmdHlp.setFooter(new StringBuffer(getToolName()
				+ " - GeoSolutions S.a.s (C) 2009 - v ").append(getVersion())
				.toString());
		cmdHlp
				.setDivider("|-------------------------------------------------------------------------|");

		// /////////////////////////////////////////////////////////////////////
		//
		// Close Parser
		//
		// /////////////////////////////////////////////////////////////////////
		optionsGroup = new GroupImpl(cmdOpts, "Options", "All the options", 1,
				cmdOpts.size());
		cmdParser.setGroup(optionsGroup);
		cmdParser.setHelpOption(helpOpt);
		cmdParser.setHelpFormatter(cmdHlp);

	}

	public boolean parseArgs(String[] args) {

		cmdLine = cmdParser.parseAndHelp(args);
		if (cmdLine == null)
			return false;

		if (cmdLine.hasOption(versionOpt)) {
			System.out.print(new StringBuffer(getToolName()).append(
					" - GeoSolutions S.a.s (C) 2009 - v").append(getVersion())
					.toString());
			System.exit(0);

		}
		return true;

	}

	public boolean hasOption(Option opt) {
		if (cmdLine == null)
			throw new IllegalStateException();
		return this.cmdLine.hasOption(opt);
	}

	public boolean hasOption(String optName) {
		if (cmdLine == null)
			throw new IllegalStateException();
		return this.cmdLine.hasOption(optName);
	}

	public Object getOptionValue(Option opt) {
		if (cmdLine == null)
			throw new IllegalStateException();
		return this.cmdLine.getValue(opt);
	}

	public Object getOptionValue(String optName) {
		if (cmdLine == null)
			throw new IllegalStateException();
		return this.cmdLine.getValue(optName);
	}

	public String getToolName() {
		return toolName;
	}

	public String getVersion() {
		return version;
	}
}

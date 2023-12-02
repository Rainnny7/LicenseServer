import { createRequire } from "node:module";

import chalk from "chalk";
import figlet from "figlet";
import inquirer from "inquirer";

const require = createRequire(import.meta.url);
const packageJson = require("../package.json");

/**
 * Print the banner to the terminal.
 */
const printBanner = () => {
	console.log();
	console.log(
		chalk.blueBright(
			figlet.textSync("License-Server  CLI", {
				font: "small",
				horizontalLayout: "default",
				verticalLayout: "default",
			})
		)
	);
	console.log();
	console.log(" " + chalk.gray(`Running version v${packageJson.version}`));
	console.log();
};

/**
 * Ask questions and return the answers.
 *
 * @returns the answers
 */
const askQuestions = () => {
	return inquirer.prompt([
		{
			name: "LICENSE_SERVER_URL",
			type: "input",
			message: "What is the URL to your license server?",
			default: "http://localhost:7500",
			validate(input) {
				// Validate the input
				if (
					/https?:\/\/(www\.)?(localhost|[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6})\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*)/.test(
						input
					)
				) {
					return true;
				}
				// Incorrect input
				return "The URL is invalid.";
			},
		},
	]);
};

// Run the application
const run = async () => {
	printBanner(); // Print the banner
	const { LICENSE_SERVER_URL } = await askQuestions(); // Ask the questions and await the response
	console.log(`server url = ${LICENSE_SERVER_URL}`);
};
run();
